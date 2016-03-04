package io.coriolis.api.tasks;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.google.common.collect.ImmutableMultimap;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
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

    public RefreshEDDBStationsTask(String stationJSONUrl, Universe universe, HttpClient httpClient) {
        super("refresh-eddb-stations");
        this.stationJSONUrl = stationJSONUrl;
        this.universe = universe;
        this.httpClient = httpClient;
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
            executionFailed("Unable to create temporary Stations JSON file");
            return;
        }

        HttpResponse response;
        try {
            response = httpClient.execute(new HttpGet(stationJSONUrl));
        } catch (IOException e) {
            logger.error("Unable to pull Stations JSON data: " + e.getMessage());
            executionFailed("Unable to pull Stations JSON data");
            return;
        }
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {
                OutputStream stream = new FileOutputStream(stationsJsonFile);
                entity.writeTo(new FileOutputStream(stationsJsonFile));
                stream.close();
            } catch (IOException e) {
                logger.error("Unable to write to temporary Stations JSON file: " + e.getMessage());
                executionFailed("Unable to write to temporary Stations JSON file");
            }
        } else {
            logger.error("Stations JSON response is empty!");
            executionFailed("Stations JSON response is empty!");
        }

        JsonFactory f = new MappingJsonFactory();
        JsonParser jp;

        try {
            jp = f.createParser(stationsJsonFile);
        } catch (IOException e) {
            logger.error("Unable to read temporary Stations JSON file: " + e.getMessage());
            executionFailed("Unable to read temporary Stations JSON file");
            return;
        }

        JsonToken current;
        int stationsUpdated = 0;

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
                            stationNode.get("has_outfitting").asBoolean(false),
                            jsonNodeToList(stationNode.get("selling_ships")),
                            jsonNodeToList(stationNode.get("selling_modules"))
                    );
                    if (s != null) {
                        stationsUpdated++;
                    }
                } else {
                    logger.warn("Station " + stationNode.get("name").asText() + " [" + stationNode.get("id").asInt() +  "] in unknown system [" + stationNode.get("system_id").asInt() + "]");
                }
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            executionFailed("NPE Error");
            return;
        }
        catch (UnknownShipException e) {
            logger.error("Unknown ship found parsing Station JSON Data: " + e.getMessage());
            executionFailed("Unknown ship found parsing Station JSON Data: " + e.getMessage());
            return;
        }
        catch (IOException e) {
            logger.error("Error parsing Station JSON data: " + e.getMessage());
            executionFailed("Error parsing Station JSON data");
            return;
        } finally {
            // TODO: Universe release write lock
            stationsJsonFile.delete();
        }

        String summary = stationsUpdated + " Stations updated";
        logger.info(summary);
        executionSucceeded(summary);
        output.write(summary);
        output.write("\n");
    }

    private List<String> jsonNodeToList(JsonNode node) {

        if(node == null || !node.isArray()) {
            return null;
        }

        List<String> list = new ArrayList<>();

        for (JsonNode n : node) {
            list.add(n.asText());
        }

        return list;
    }
}
