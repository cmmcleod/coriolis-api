package io.coriolis.api.tasks;

import io.dropwizard.servlets.tasks.Task;
import org.glassfish.jersey.message.internal.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;

public abstract class RunnableMonitoredTask extends Task implements Runnable {

    static Logger logger = LoggerFactory.getLogger(RunnableMonitoredTask.class);

    private boolean lastExecutionSuccess;
    private String statusMessage;

    protected RunnableMonitoredTask(String name) {
        super(name);
        statusMessage = "never run";
        lastExecutionSuccess = true;    // Healthy until run
    }

    protected void executing() {
        lastExecutionSuccess = true;
        statusMessage = "executing";
    }

    protected void executionFailed(String failureMessage) {
        lastExecutionSuccess = false;
        this.statusMessage = failureMessage;
    }

    protected void executionSucceeded() {
        executionSucceeded(null);
    }

    protected void executionSucceeded(String statusMessage) {
        this.statusMessage = statusMessage;
        lastExecutionSuccess = true;
    }

    public boolean lastExecutionSucceeded() {
        return lastExecutionSuccess;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public void run() {
        try {
            logger.info("Running scheduled task: " + getName());
            execute(null, new PrintWriter(new NullOutputStream()));
            logger.info("Finished scheduled task: " + getName());
        } catch (Exception e) {
            executionFailed("Uknown/Uncaught Error");
            logger.error("Task " + this.getName() + " uncaught error: " + e.getMessage());
        }
    }

}
