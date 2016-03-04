package io.coriolis.api.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import io.coriolis.api.core.frontier.CompanionClient;
import io.coriolis.api.resources.exceptions.JsonWebApplicationException;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Path("/companion")
@Produces(MediaType.APPLICATION_JSON)
@CacheControl(noCache = true, maxAge = 0)
public class CompanionEndpoint {

    final static Logger logger = LoggerFactory.getLogger(CompanionEndpoint.class);

    CompanionClient companionClient;

    public CompanionEndpoint(CompanionClient companionClient) {
        this.companionClient = companionClient;
    }

    @POST
    @Timed
    @Path("/login")
    public Response login(@FormParam("email") String email, @FormParam("password") String password) {

        if(Strings.isNullOrEmpty(email) || Strings.isNullOrEmpty(password)) {
            throw new JsonWebApplicationException("Email and Password required", Response.Status.BAD_REQUEST);
        }

        CookieStore cookieStore = companionClient.login(email, password);
        Response.ResponseBuilder responseBuilder = Response.ok();

        for (NewCookie c : fromCompanionCookies(cookieStore)) {
            logger.debug("Login cookies: " + c.getName() + " " + c.getValue());
            responseBuilder.cookie(c);
        }

        return responseBuilder.build();
    }

    @POST
    @Timed
    @Path("/confirm")
    public Response confirm(@FormParam("code") String code, @Context HttpServletRequest request) {
        CookieStore cookies = companionClient.confirm(code, toCompanionCookies(request.getCookies(), true));
        Response.ResponseBuilder responseBuilder = Response.ok();

        for (NewCookie c : fromCompanionCookies(cookies)) {
            logger.debug("Confirm Cookies: " + c.getName() + " " + c.getValue());
            responseBuilder.cookie(c);
        }

        return responseBuilder.build();
    }

    @GET
    @Timed
    @Path("/profile")
    public InputStream getProfile(@Context HttpServletRequest request) throws IOException {
       return companionClient.getProfile(toCompanionCookies(request.getCookies(), false));
    }

    private List<NewCookie> fromCompanionCookies(CookieStore cookieStore) {
        List<Cookie> cookieList = cookieStore.getCookies();

        NewCookie mid = null;
        String companionAppValue = null;

        List<NewCookie> cookies = new ArrayList<>(cookieList.size());

        for (Cookie c : cookieList) {
            if (c.getName().equalsIgnoreCase("CompanionApp")) {
                companionAppValue = c.getValue();
            } else {
                int maxAge = 100000; //(int) new Duration(new DateTime(), new DateTime(c.getExpiryDate())).getStandardSeconds()
                NewCookie nc = new NewCookie(c.getName(), c.getValue(), "/", null, null, maxAge, false, false);
                if (c.getName().equalsIgnoreCase("mid")) {
                    mid = nc;
                }
                cookies.add(nc);
            }

        }

        if(mid == null || companionAppValue == null) {
            throw new JsonWebApplicationException("No Companion session", Response.Status.UNAUTHORIZED);
        }
        cookies.add(new NewCookie("CompanionApp", companionAppValue, "/", null, null, mid.getMaxAge(), false, false));

        return cookies;
    }

    private CookieStore toCompanionCookies(javax.servlet.http.Cookie[] cookies, boolean excludeMtk) {
        CookieStore companionCookies = new BasicCookieStore();
        boolean hasCompanionApp = false, hasMid = false;

        for (javax.servlet.http.Cookie c : cookies) {

            switch (c.getName()) {
                case "CompanionApp":
                    hasCompanionApp = true;
                    break;
                case "mid":
                    hasMid = true;
                    break;
                case "mtk":
                    if (excludeMtk) {
                        continue;
                    }
            }

            BasicClientCookie bcc = new BasicClientCookie(c.getName(), c.getValue());
            bcc.setPath("/");
            bcc.setDomain(CompanionClient.COMPANION_HOSTNAME);
            companionCookies.addCookie(bcc);
        }

        if(!hasCompanionApp || !hasMid) {
            throw new JsonWebApplicationException("No Companion session", Response.Status.UNAUTHORIZED);
        }

        return companionCookies;
    }
}
