package io.coriolis.api.tasks;

import com.google.common.collect.ImmutableMultimap;
import io.coriolis.api.core.eddn.EDDNManager;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;

public class RestartEDDNListenerTask extends Task {

    private EDDNManager eddnManager;

    public RestartEDDNListenerTask(EDDNManager eddnManager) {
        super("restart-eddn-listener");
        this.eddnManager = eddnManager;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        try {
            eddnManager.restart();
        } catch (Exception e) {
            output.write("Unabled to restart EDDN Listener: " + e.getMessage());
            return;
        }
        output.write("ok");
    }
}
