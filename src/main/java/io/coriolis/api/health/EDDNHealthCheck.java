package io.coriolis.api.health;

import com.codahale.metrics.health.HealthCheck;
import io.coriolis.api.core.eddn.EDDNManager;

/**
 * Created by cmmcleod on 10/19/15.
 */
public class EDDNHealthCheck extends HealthCheck {

    private EDDNManager eddnManager;

    public EDDNHealthCheck(EDDNManager eddnManager) {
        this.eddnManager = eddnManager;
    }

    @Override
    protected Result check() throws Exception {
        if (eddnManager.isRunning()) {
            return Result.healthy();
        } else {
            return Result.unhealthy("EDDN Listener has stopped");
        }
    }
}
