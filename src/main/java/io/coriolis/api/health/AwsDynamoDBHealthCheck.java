package io.coriolis.api.health;

import com.codahale.metrics.health.HealthCheck;
import io.coriolis.api.core.aws.AmazonDynamoDBClientManager;
import io.coriolis.api.core.eddn.EDDNManager;

/**
 * Created by cmmcleod on 10/19/15.
 */
public class AwsDynamoDBHealthCheck extends HealthCheck {

    private AmazonDynamoDBClientManager dbClientManager;

    public AwsDynamoDBHealthCheck(AmazonDynamoDBClientManager dbClientManager) {
        this.dbClientManager = dbClientManager;
    }

    @Override
    protected Result check() throws Exception {
        if (dbClientManager.isHealthy()) {
            return Result.healthy();
        } else {
            return Result.unhealthy("DynamoDB is failing");
        }
    }
}
