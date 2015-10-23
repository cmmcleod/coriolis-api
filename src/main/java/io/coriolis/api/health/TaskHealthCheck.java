package io.coriolis.api.health;

import com.codahale.metrics.health.HealthCheck;
import io.coriolis.api.tasks.RunnableMonitoredTask;

/**
 * Created by cmmcleod on 10/19/15.
 */
public class TaskHealthCheck extends HealthCheck {

    private RunnableMonitoredTask task;

    public TaskHealthCheck(RunnableMonitoredTask task) {
        this.task = task;
    }

    @Override
    protected Result check() throws Exception {
        if (task.lastExecutionSucceeded()) {
            if (task.getStatusMessage() == null) {
                return Result.healthy();
            }
            return Result.healthy(task.getStatusMessage());
        } else {
            return Result.unhealthy(task.getStatusMessage());
        }
    }
}
