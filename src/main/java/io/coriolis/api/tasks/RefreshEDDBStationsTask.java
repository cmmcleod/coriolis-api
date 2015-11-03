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
import io.coriolis.api.entities.Station;
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

public class RefreshEDDBStationsTask extends RunnableMonitoredTask {

    final static Logger logger = LoggerFactory.getLogger(RefreshEDDBStationsTask.class);

    private Universe universe;
    private String stationJSONUrl;
    private HttpClient httpClient;
    private DynamoDBMapper dbMapper;

    public RefreshEDDBStationsTask(String stationJSONUrl, Universe universe, HttpClient httpClient, AmazonDynamoDBClient dbClient) {
        super("refresh-eddb-stations");
        this.stationJSONUrl = stationJSONUrl;
        this.universe = universe;
        this.httpClient = httpClient;
        dbMapper = new DynamoDBMapper(dbClient);
    }

    @Timed
    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        executing();
        File stationsJsonFile;
        try {
            stationsJsonFile = File.createTempFile("eddb-stations", "json");
            logger.debug("Temporary System JSON File created: " + stationsJsonFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Unable to create temporary Stations JSON file: " + e.getMessage());
            // TODO: healthcheck failed
            return;
        }

        HttpResponse response;
        try {
            response = httpClient.execute(new HttpGet(stationJSONUrl));
        } catch (IOException e) {
            logger.error("Unable to pull Stations JSON data: " + e.getMessage());
            // TODO: healthcheck failed
            return;
        }
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                OutputStream stream = new FileOutputStream(stationsJsonFile);
                entity.writeTo(new FileOutputStream(stationsJsonFile));
                stream.close();
            } catch (IOException e) {
                // TODO: healthcheck failed
                logger.error("Unable to write to temporary Stations JSON file: " + e.getMessage());
            }
        } else {
            // TODO: healthcheck failed
            logger.error("Stations JSON response is empty!");
        }

        JsonFactory f = new MappingJsonFactory();
        JsonParser jp;

        try {
            jp = f.createParser(stationsJsonFile);
        } catch (IOException e) {
            // TODO: healthcheck failed
            logger.error("Unable to read temporary Stations JSON file: " + e.getMessage());
            return;
        }

        JsonToken current;
        List<Station> stationsToSave = new ArrayList<>();

        try {
            current = jp.nextToken();
            if (current != JsonToken.START_ARRAY) {
                throw new JsonParseException("System JSON does not start with an Array!", jp.getCurrentLocation());
            }
            // TODO: Universe write lock
            while (jp.nextToken() != JsonToken.END_ARRAY) {
                JsonNode stationNode = jp.readValueAsTree();
                StarSystem system = universe.getSystem(stationNode.get("system_id").asInt());

                if(system != null) {
                    Station s = universe.updateStationFromEDDB(
                            system,
                            stationNode.get("id").asInt(),
                            stationNode.get("name").asText(),
                            stationNode.get("distance_to_star").asInt(),
                            stationNode.get("allegiance").asText(),
                            stationNode.get("max_landing_pad_size").asText(),
                            stationNode.get("type").asText(),
                            stationNode.get("has_shipyard").asBoolean(false),
                            stationNode.get("has_outfitting").asBoolean(false)
                    );
                    if (s != null) {
                        stationsToSave.add(s);
                    }
                } else {
                    logger.warn("Station " + stationNode.get("name").asText() + " [" + stationNode.get("id").asInt() +  "] in unknown system [" + stationNode.get("system_id").asInt() + "]");
                }
            }

        } catch (IOException e) {
            // TODO: healthcheck failed
            logger.error("Error parsing Station JSON data: " + e.getMessage());
            return;
        } finally {
            // TODO: Universe release write lock
            stationsJsonFile.delete();
        }

        // Save updated stations to the database
        if(stationsToSave.size() > 0) {
            logger.info("Saving " + stationsToSave.size() + " Stations to the Database");
            try {
                dbMapper.batchSave(stationsToSave);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String summary = stationsToSave.size() + " Stations updated in the Database";
        logger.info(summary);
        executionSucceeded(summary);
        output.write(summary);
        output.write("\n");
    }
}
