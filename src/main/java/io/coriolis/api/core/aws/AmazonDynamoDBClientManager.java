package io.coriolis.api.core.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.Tables;
import io.coriolis.api.ApiConfiguration;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmazonDynamoDBClientManager implements Managed {

    final static Logger logger = LoggerFactory.getLogger(AmazonDynamoDBClientManager.class);

    private AmazonDynamoDBClient client;
    private boolean healthy = false;

    public AmazonDynamoDBClientManager(ApiConfiguration conf) throws InterruptedException {
        client = new AmazonDynamoDBClient(new BasicAWSCredentials(conf.getAwsAccessKey(), conf.getAwsSecretKey()));
        client.setEndpoint(conf.getDynamoDBEndpoint());
        DynamoDB db = new DynamoDB(client);

        if (!Tables.doesTableExist(client, "Stations")) {
            CreateTableRequest stationRequest = new CreateTableRequest()
                    .withTableName("Stations")
                    .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.N));
            stationRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1000L).withWriteCapacityUnits(500L));
            db.createTable(stationRequest).waitForActive();
            logger.info("Stations table created");
        }
        if (!Tables.doesTableExist(client, "Systems")) {
            CreateTableRequest systemRequest = new CreateTableRequest()
                    .withTableName("Systems")
                    .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                    .withAttributeDefinitions(new AttributeDefinition().withAttributeName("id").withAttributeType(ScalarAttributeType.N));
            systemRequest.setProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1000L).withWriteCapacityUnits(500L));
            db.createTable(systemRequest).waitForActive();
            logger.info("Systems table created");
        }
        healthy = true;
    }

    public AmazonDynamoDBClient getClient() {
        return client;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void start() throws Exception { }

    @Override
    public void stop() throws Exception {
        client.shutdown();
    }
}
