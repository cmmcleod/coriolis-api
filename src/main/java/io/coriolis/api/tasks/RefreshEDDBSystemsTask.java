package io.coriolis.api.tasks;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.google.common.collect.ImmutableMultimap;
import io.coriolis.api.entities.StarSystem;
import io.coriolis.api.core.Universe;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RefreshEDDBSystemsTask extends RunnableMonitoredTask {

    final static Logger logger = LoggerFactory.getLogger(RefreshEDDBSystemsTask.class);

    private Universe universe;
    private String systemJSONUrl;
    private HttpClient httpClient;
    private DynamoDBMapper dbMapper;

    public RefreshEDDBSystemsTask(String systemJSONUrl, Universe universe, HttpClient httpClient, AmazonDynamoDBClient dbClient) {
        super("refresh-eddb-systems");
        this.systemJSONUrl = systemJSONUrl;
        this.universe = universe;
        this.httpClient = httpClient;
        dbMapper = new DynamoDBMapper(dbClient);
    }

    @Timed
    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        executing();

        File systemsJsonFile;
        try {
            systemsJsonFile = File.createTempFile("eddb-systems", "json");
            logger.debug("Temporary System JSON File created: " + systemsJsonFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Unable to create temporary Systems JSON file: " + e.getMessage());
            executionFailed("Unable to create temporary Systems JSON file");
            return;
        }

        HttpResponse response;
        try {
            response = httpClient.execute(new HttpGet(systemJSONUrl));
        } catch (IOException e) {
            logger.error("Unable to pull Systems JSON data: " + e.getMessage());
            executionFailed("Unable to pull Systems JSON data");
            return;
        }
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                OutputStream stream = new FileOutputStream(systemsJsonFile);
                entity.writeTo(new FileOutputStream(systemsJsonFile));
                stream.close();
            } catch (IOException e) {
                logger.error("Unable to write to temporary Systems JSON file: " + e.getMessage());
                executionFailed("Unable to write to temporary Systems JSON file");
            }
        } else {
            logger.error("Systems JSON response is empty!");
            executionFailed("Systems JSON response is empty");
        }

        JsonFactory f = new MappingJsonFactory();
        JsonParser jp;

        try {
            jp = f.createParser(systemsJsonFile);
        } catch (IOException e) {
            logger.error("Unable to read temporary System JSON file: " + e.getMessage());
            executionFailed("Unable to read temporary System JSON file");
            return;
        }

        JsonToken current;
        List<StarSystem> systemToSave = new ArrayList<>();

        try {
            current = jp.nextToken();
            if (current != JsonToken.START_ARRAY) {
                throw new JsonParseException("System JSON does not start with an Array!", jp.getCurrentLocation());
            }

            // TODO: Universe write lock

            while (jp.nextToken() != JsonToken.END_ARRAY) {
                JsonNode systemNode = jp.readValueAsTree();
                StarSystem s = universe.updateSystemFromEDDB(
                        systemNode.get("id").asInt(),
                        systemNode.get("name").asText(),
                        systemNode.get("x").asDouble(),
                        systemNode.get("y").asDouble(),
                        systemNode.get("z").asDouble(),
                        systemNode.get("needs_permit").asBoolean(false)
                );
                if (s != null) {
                    systemToSave.add(s);
                }
            }
        } catch (IOException e) {
            logger.error("Error parsing System JSON data: " + e.getMessage());
            executionFailed("Error parsing System JSON data");
            return;
        } finally {
            // TODO: Universe release write lock
            systemsJsonFile.delete();
        }

        // Save updated systems to the database
        if(systemToSave.size() > 0) {
            logger.info("Saving " + systemToSave.size() + " Systems to the Database");
            dbMapper.batchSave(systemToSave);
        }
        String summary = systemToSave.size() + " Systems updated in the Database";
        logger.info(summary);
        executionSucceeded(summary);
        output.write(summary);
        output.write("\n");
    }
}
