package io.coriolis.api.core.eddn;


import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.coriolis.api.core.Universe;
import io.coriolis.api.core.modules.*;
import io.coriolis.api.core.modules.exceptions.UnknownModuleException;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by cmmcleod on 10/12/15.
 */
public class EDDNListener implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(EDDNListener.class);

    private static int MAX_BYTE_SIZE = 524288; // 512 KB
    private static int EDDN_TIMEOUT = 300000; // 5 minute timeout
    private static int RECONNECT_WAIT = 15000; // 15 second wait, used for incremental back-off
    private static String SCHEMA_REF = "\"$schemaRef\":";

    private final Meter parseErrors;
    private final Meter npeErrors;
    private final Meter messageMeter;
    private final Meter shipyardMeter;
    private final Meter outfittingMeter;
    private final Meter unknownShip;
    private final Meter unknownModule;
    private final Meter zMQErrors;

    private ZMQ.Context context;
    private Inflater inflater;
    private String host;
    private String url;
    private int port;
    private ObjectMapper mapper;
    private Universe universe;
    private Modules modules;
    private int reconnectAttempt;
    private boolean retryConnection;
    private boolean connected;

    public EDDNListener (ZMQ.Context context, String host, int port, Universe universe, MetricRegistry metrics) {
        this.context = context;
        this.host = host;
        this.port = port;
        this.url = "tcp://" + host + ":" + port;
        inflater = new Inflater();
        this.universe = universe;
        modules = Modules.INSTANCE;
        this.parseErrors = metrics.meter("EDDN.parseErrors");
        this.npeErrors = metrics.meter("EDDN.npeErrors");
        this.messageMeter = metrics.meter("EDDN.messages");
        this.shipyardMeter = metrics.meter("EDDN.shipyard");
        this.outfittingMeter = metrics.meter("EDDN.outfitting");
        this.unknownShip = metrics.meter("EDDN.unknownShip");
        this.unknownModule = metrics.meter("EDDN.unknownModule");
        this.zMQErrors = metrics.meter("zMQErrors");
        mapper = new ObjectMapper();
        retryConnection = true;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[MAX_BYTE_SIZE];
        int msgLength;

        while (retryConnection && !Thread.currentThread().isInterrupted()) {    // Reconnect loop
            logger.info("Connecting to EDDN ZeroMQ Service: " + host + ":" + port);
            ZMQ.Socket socket = context.socket(ZMQ.SUB);
            socket.setHWM(1000);
            socket.setLinger(0);
            socket.setReceiveTimeOut(EDDN_TIMEOUT);   // 2 Minute timeout
            socket.subscribe(ZMQ.SUBSCRIPTION_ALL);

            try {
                socket.connect(url);
                setConnected(true);
                logger.info("Listening to EDDN");

                while (!Thread.currentThread().isInterrupted()) {   // Listener Loop

                    try {
                        byte[] data = socket.recv(0);

                        // If no messages have been received the connection may have died
                        if (data == null) { // Timeout, reconnect
                            logger.debug("Reconnecting to EDDN");
                            socket.disconnect(url);
                            socket.connect(url);
                            continue;
                        }

                        inflater.setInput(data);
                        msgLength = inflater.inflate(buffer);
                        inflater.reset();
                        messageMeter.mark();

                        String msg = new String(buffer, 0, msgLength, StandardCharsets.UTF_8);
                        int schemaRefStart = msg.indexOf(SCHEMA_REF);

                        if (schemaRefStart > 0) {   // Message contains schemaref
                            schemaRefStart += SCHEMA_REF.length();
                            if (msg.indexOf("\"http://schemas.elite-markets.net/eddn/shipyard/1\"", schemaRefStart) >= 0) {
                                JsonNode node = mapper.readTree(msg).get("message");
                                try {
                                    universe.updateStationFromEDDN(
                                            node.get("systemName").asText(),
                                            node.get("stationName").asText(),
                                            nodeToList(node.get("ships"))
                                    );
                                    outfittingMeter.mark();
                                } catch (UnknownShipException e) {
                                    unknownShip.mark();
                                    logger.error("Unknown ship from EDDN: " + e.getMessage());
                                }
                            } else if (msg.indexOf("\"http://schemas.elite-markets.net/eddn/outfitting/1\"", schemaRefStart) >= 0) {
                                JsonNode node = mapper.readTree(msg).get("message");
                                JsonNode modulesNode = node.get("modules");
                                boolean hasStandard = false, hasInternal = false, hasHardpoints = false, hasUtilities = false;
                                ModuleSet standardSet = modules.createStandardSet();
                                ModuleSet internalSet = modules.createInternalSet();
                                ModuleSet hardpointSet = modules.createHardpointSet();
                                ModuleSet utilitySet = modules.createUtilitySet();

                                try {
                                    for (JsonNode n : modulesNode) {
                                        switch (n.get("category").asText()) {
                                            case "standard":
                                                standardSet.add(modules.getStandardIndexBy(
                                                        n.get("name").asText(),
                                                        n.get("class").asText(),
                                                        n.get("rating").asText(),
                                                        n.has("ship") ? n.get("ship").asText() : null
                                                ));
                                                hasStandard = true;
                                                break;
                                            case "internal":
                                                internalSet.add(modules.getInternalIndexBy(
                                                        n.get("name").asText(),
                                                        n.get("class").asText(),
                                                        n.get("rating").asText()
                                                ));
                                                hasInternal = true;
                                                break;
                                            case "hardpoint":
                                                hardpointSet.add(modules.getHardpointIndexBy(
                                                        n.get("name").asText(),
                                                        n.get("class").asText(),
                                                        n.get("rating").asText(),
                                                        n.has("mount") ? n.get("mount").asText().substring(0, 1) : "",
                                                        n.has("guidance") ? n.get("guidance").asText().substring(0, 1) : ""
                                                ));
                                                hasHardpoints = true;
                                                break;
                                            case "utility":
                                                utilitySet.add(modules.getUtilityIndexBy(
                                                        n.get("name").asText(),
                                                        n.get("class").asText(),
                                                        n.get("rating").asText()
                                                ));
                                                hasUtilities = true;
                                                break;
                                            default:
                                                logger.error("Unknown module category:" + n.get("category").asText());
                                        }
                                    }

                                    universe.updateStationFromEDDN(
                                            node.get("systemName").asText(),
                                            node.get("stationName").asText(),
                                            hasStandard ? standardSet : null,
                                            hasInternal ? internalSet : null,
                                            hasHardpoints ? hardpointSet : null,
                                            hasUtilities ? utilitySet : null
                                    );
                                    shipyardMeter.mark();
                                } catch (UnknownModuleException e) {
                                    unknownModule.mark();
                                    logger.error("Unknown module from EDDN: " + e.getMessage());
                                } catch (NullPointerException e) {
                                    npeErrors.mark();
                                    logger.error("NPE when parsing  JSON: " + e.getMessage());
                                } catch (UnknownShipException e) {
                                    unknownShip.mark();
                                    logger.error("Unknown ship from EDDN: " + e.getMessage());
                                }
                            } else {
                                //logger.debug("Discarding message of type: " + msg.substring(schemaRefStart + 1, Math.max(msg.indexOf("\"",schemaRefStart + 2),0)));
                                logger.debug("Discarding irrelevant message");
                            }
                        }
                        reconnectAttempt = 0;   // Successfully received a message without errors. Reset backoff
                    } catch (DataFormatException e) {
                        parseErrors.mark();
                        logger.warn("Unable to decompress EDDN message");
                    } catch (JsonProcessingException e) {
                        parseErrors.mark();
                        logger.warn("JSON Parse Error: Unable to process EDDN message");
                    } catch (IOException e) {
                        parseErrors.mark();
                        logger.error("EDDN IO Error: " + e.getMessage());
                    }
                }   // Listener Loop End
            } catch (ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                    retryConnection = false;
                    break;  // Stop listening / break while loop
                } else {
                    zMQErrors.mark();
                    logger.error("ZeroMQ Error: [" + ZMQ.Error.findByCode(e.getErrorCode()) + "] " + e.getMessage());
                    try {
                        logger.warn("EDDN reconnect attempt:" + reconnectAttempt + ", waiting" + (RECONNECT_WAIT * reconnectAttempt / 1000) + "seconds");
                        Thread.sleep(RECONNECT_WAIT * (long)Math.pow(2, reconnectAttempt));
                        reconnectAttempt++;
                    } catch (InterruptedException e1) {
                        logger.error("Unable to wait for EDDN reconnection attempt " + reconnectAttempt);
                        retryConnection = false;
                    }
                }
            } finally {
                socket.close();
                connected = false;
                logger.info("STOPPED listening to EDDN");
            }
        }   // Reconnect loop end
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    private synchronized void setConnected(boolean connected) {
        this.connected = connected;
    }

    private static List<String> nodeToList(JsonNode node) {
        if (!node.isArray()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        for (JsonNode n : node) {
            list.add(n.asText());
        }
        return list;
    }

}
