package io.coriolis.api;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import io.coriolis.api.core.Universe;
import io.coriolis.api.core.eddn.EDDNManager;
import io.coriolis.api.core.frontier.CompanionClient;
import io.coriolis.api.core.modules.Modules;
import io.coriolis.api.health.EDDNHealthCheck;
import io.coriolis.api.health.TaskHealthCheck;
import io.coriolis.api.resources.CompanionEndpoint;
import io.coriolis.api.resources.FindBuildEndpoint;
import io.coriolis.api.resources.SystemEndpoint;
import io.coriolis.api.tasks.RefreshEDDBStationsTask;
import io.coriolis.api.tasks.RefreshEDDBSystemsTask;
import io.coriolis.api.tasks.RestartEDDNListenerTask;
import io.dropwizard.Application;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Environment;
import org.apache.http.client.HttpClient;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.util.EnumSet;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiApplication extends Application<ApiConfiguration> {

    final static Logger logger = LoggerFactory.getLogger(ApiApplication.class);

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
        new ApiApplication().run(args);
    }

    @Override
    public String getName() {
        return "Coriolis API";
    }

    @Override
    public void run(ApiConfiguration conf, Environment env) throws InterruptedException {
        final HttpClient httpClient = new HttpClientBuilder(env).using(conf.getHttpClientConfiguration()).build(getName());
        final HttpClient companionHttpClient = new HttpClientBuilder(env).using(conf.getCompanionClientConfiguration()).build("Companion");
        final MetricRegistry metricRegistry = env.metrics();
        //AmazonDynamoDBClientManager managedDynamoDBClient = new AmazonDynamoDBClientManager(conf);
        Universe universe = new Universe(metricRegistry);
        EDDNManager eddnManager = new EDDNManager(conf.getEddnHost(), conf.getEddnPort(), universe, metricRegistry);
        RefreshEDDBSystemsTask refresSystemsTask = new RefreshEDDBSystemsTask(conf.getEddbSystemJSONUrl(), universe, httpClient);
        RefreshEDDBStationsTask refreshStationsTask = new RefreshEDDBStationsTask(conf.getEddbStationJSONUrl(), universe, httpClient);

        // Determine next EDDB Update time
        DateTime eddbUpdateTime = new DateTime().withHourOfDay(2).withMinuteOfHour(15);
        if(!eddbUpdateTime.isAfterNow()) {
            eddbUpdateTime = eddbUpdateTime.plusDays(1);
        }
        long minsUntilEDDBUpdate = Minutes.minutesBetween(new DateTime(), eddbUpdateTime.isAfterNow() ? eddbUpdateTime : eddbUpdateTime.plusDays(1)).getMinutes();

        // Initialize Systems, Stations, Modules, etc
        Modules.INSTANCE.initialize();
        //universe.loadFromDB();
        //refresSystemsTask.run();
        //refreshStationsTask.run();

        // Life Cycle / Managed Objects
        //env.lifecycle().manage(eddnManager);
        //env.lifecycle().manage(managedDynamoDBClient);

        // Healthchecks
        env.healthChecks().register("EDDN", new EDDNHealthCheck(eddnManager));
        env.healthChecks().register("RefreshStations", new TaskHealthCheck(refreshStationsTask));
        env.healthChecks().register("RefreshSystems", new TaskHealthCheck(refresSystemsTask));
        //env.healthChecks().register("DynamoDB", new AwsDynamoDBHealthCheck(managedDynamoDBClient));

        // Admin Interface
        env.admin().setSecurityHandler(new AdminConstraintSecurityHandler(conf.getAdminUser(), conf.getAdminPassword()));
        env.admin().addServlet("adminInterface", new AssetServlet("/assets/admin", "/admin", "index.html", Charsets.UTF_8)).addMapping("/admin");

        // Admin Tasks
        env.admin().addTask(new RestartEDDNListenerTask(eddnManager));
        env.admin().addTask(refresSystemsTask);
        env.admin().addTask(refreshStationsTask);

        // Consumer/User Endpoints
        enableCORS(env.servlets(), "/*");
        env.jersey().register(new CompanionEndpoint(new CompanionClient(companionHttpClient)));
        env.jersey().register(new SystemEndpoint(universe));
        env.jersey().register(new FindBuildEndpoint(universe));

        // Scheduled Tasks
        logger.info("Next EDDB scheduled update @ " + eddbUpdateTime + " in " + minsUntilEDDBUpdate + " minutes");
        ScheduledExecutorService ses = env.lifecycle().scheduledExecutorService("EDDB-tasks").build();
        ses.scheduleAtFixedRate(refresSystemsTask, minsUntilEDDBUpdate, 1440, TimeUnit.MINUTES); // Wait until 02:15 UTC
        ses.scheduleAtFixedRate(refreshStationsTask, minsUntilEDDBUpdate + 5, 1440, TimeUnit.MINUTES); // Wait until 02:20 UTC

        // Remove uninteresting metrics on Jetty startup
        env.lifecycle().addServerLifecycleListener(filterUnhelpfulMetrics(metricRegistry));
    }

    /**
     * Enable CORS headers for public (i.e. non-admin) endpoints
     */
    private void enableCORS(ServletEnvironment servletEnvironment, String path) {
        FilterRegistration.Dynamic cors = servletEnvironment.addFilter("CORS", CrossOriginFilter.class);
        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin");
        cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,HEAD");
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, path);
    }

    /**
     * @return A mondane listener to filter metrics / remove unhelpful/uninteresting metrics on Jetty start-up
     */
    private ServerLifecycleListener filterUnhelpfulMetrics(final MetricRegistry metricRegistry){
        return new ServerLifecycleListener() {
            @Override
            public void serverStarted(Server server) {
                metricRegistry.removeMatching(new MetricFilter() {
                    @Override
                    public boolean matches(String s, Metric metric) {
                        if(s.startsWith("io.dropwizard.jetty.MutableServletContextHandler.percent")) {
                            return false;
                        }
                        if (s.startsWith("jvm.buffers")
                                || s.startsWith("jvm.classloader")
                                || s.startsWith("jvm.filedescriptor")
                                || s.startsWith("jvm.memory.pools")
                                || s.startsWith("jvm.threads")
                                || s.startsWith("org.apache.http.conn")
                                || s.startsWith("ch.qos")
                                || s.startsWith("io.dropwizard.jetty")
                                || s.startsWith("org.eclipse") ) {
                            return true;
                        }
                        return false;
                    }
                });
            }
        };
    }

    private class AdminConstraintSecurityHandler extends ConstraintSecurityHandler {

        private static final String ADMIN_ROLE = "admin";

        public AdminConstraintSecurityHandler(final String userName, final String password) {
            final Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, ADMIN_ROLE);
            constraint.setAuthenticate(true);
            constraint.setRoles(new String[]{ADMIN_ROLE});
            final ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec("/*");
            setAuthenticator(new BasicAuthenticator());
            addConstraintMapping(cm);
            setLoginService(new AdminMappedLoginService(userName, password, ADMIN_ROLE));
        }
    }

    private class AdminMappedLoginService extends MappedLoginService {

        public AdminMappedLoginService(final String userName, final String password, final String role) {
            putUser(userName, new Password(password), new String[]{role});
        }

        @Override
        public String getName() {
            return "Admin Login";
        }

        @Override
        protected UserIdentity loadUser(final String username) {
            return null;
        }

        @Override
        protected void loadUsers() throws IOException { }
    }
}
