package io.coriolis.api.core;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.coriolis.api.core.modules.*;
import io.coriolis.api.core.modules.exceptions.UnknownIdException;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@DynamoDBTable(tableName="Stations")
public class Station {

    final static Logger logger = LoggerFactory.getLogger(Station.class);

    @JsonIgnore
    private int id;
    @JsonIgnore
    private int systemId;
    @JsonProperty
    private String stationName;
    @JsonProperty
    private Integer distanceLs;
    @JsonProperty
    private String allegiance;
    @JsonProperty
    private String padSize;
    @JsonProperty
    private String stationType;
    @JsonProperty
    private boolean hasShipyard;
    @JsonProperty
    private boolean hasOutfitting;

    @JsonIgnore
    private DateTime lastUpdated;
    @JsonIgnore
    private ModuleSet standardModules;
    @JsonIgnore
    private ModuleSet internalModules;
    @JsonIgnore
    private ModuleSet hardpointModules;
    @JsonIgnore
    private ModuleSet utilityModules;
    @JsonIgnore
    private EnumSet<Ship> ships;


    public Station() {
        this(-1, -1, null, -1, null, null, null, false, false, null);
    }

    public Station(int id, int systemId, String name, Integer distanceLs, String allegiance,
                   String padSize, String type, boolean hasShipyard, boolean hasOutfitting, DateTime lastUpdated) {
        this.id = id;
        this.stationName = name;
        this.systemId = systemId;
        this.distanceLs = distanceLs;
        this.allegiance = allegiance;
        this.stationType = type;
        this.lastUpdated = lastUpdated;
        this.padSize = padSize;
        this.hasShipyard = hasShipyard;
        this.hasOutfitting = hasOutfitting;
        ships = EnumSet.noneOf(Ship.class);
        standardModules = null;
        internalModules = null;
        hardpointModules = null;
        utilityModules = null;
    }

    @DynamoDBHashKey(attributeName = "id")
    public int getId() {
        return id;
    }

    @DynamoDBAttribute(attributeName = "stationName")
    public String getStationName() {
        return stationName;
    }

    @DynamoDBAttribute(attributeName = "systemId")
    public int getSystemId() {
        return systemId;
    }

    @DynamoDBAttribute(attributeName = "distanceLs")
    public Integer getDistanceLs() {
        return distanceLs;
    }

    @JsonProperty("ships")
    @DynamoDBAttribute(attributeName = "ships")
    public List<String> getShips() {
        List<String> shipList = new ArrayList<>();
        for(Ship s : ships) {
            shipList.add(s.toString());
        }

        return shipList;
    }

    @DynamoDBAttribute(attributeName = "stationType")
    public String getStationType() {
        return stationType;
    }

    @JsonProperty("lastUpdated")
    @DynamoDBAttribute(attributeName = "lastUpdated")
    public String getLastUpdated() {
        return lastUpdated.toString();
    }

    @DynamoDBAttribute(attributeName = "allegiance")
    public String getAllegiance() {
        return allegiance;
    }

    @DynamoDBAttribute(attributeName = "padSize")
    public String getPadSize() {
        return padSize;
    }

    @DynamoDBIgnore
    public boolean hasShip(Ship ship) {
        return ships != null && ships.contains(ship);
    }

    @JsonIgnore
    @DynamoDBIgnore
    public ModuleSet getStandardSet() {
        return standardModules;
    }

    @JsonIgnore
    @DynamoDBIgnore
    public ModuleSet getInternalSet() {
        return internalModules;
    }

    @JsonIgnore
    @DynamoDBIgnore
    public ModuleSet getHardpointSet() {
        return hardpointModules;
    }

    @JsonIgnore
    @DynamoDBIgnore
    public ModuleSet getUtilitySet() {
        return utilityModules;
    }

    @JsonProperty("standardModules")
    @DynamoDBAttribute(attributeName = "standardModules")
    public List<String> getStandardModules() {
        if (standardModules != null) {
            return Modules.INSTANCE.toStandardList(standardModules);
        } else {
            return null;
        }
    }

    @JsonProperty("internalModules")
    @DynamoDBAttribute(attributeName = "internalModules")
    public List<String> getInternalModules() {
        if (internalModules != null) {
            return Modules.INSTANCE.toInternalList(internalModules);
        } else {
            return null;
        }
    }

    @JsonProperty("hardpointModules")
    @DynamoDBAttribute(attributeName = "hardpointModules")
    public List<String> getHardpointModules() {
        if (hardpointModules != null) {
            return Modules.INSTANCE.toHardPointList(hardpointModules);
        } else {
            return null;
        }
    }

    @JsonProperty("utilityModules")
    @DynamoDBAttribute(attributeName = "utilityModules")
    public List<String> getUtilityModules() {
        if (utilityModules != null) {
            return Modules.INSTANCE.toUtilityList(utilityModules);
        } else {
            return null;
        }
    }

    @DynamoDBAttribute(attributeName = "hasShipyard")
    public boolean getHasShipyard() {
        return hasShipyard;
    }

    @DynamoDBAttribute(attributeName = "hasOutfitting")
    public boolean getHasOutfitting() {
        return hasOutfitting;
    }

    @JsonIgnore
    @DynamoDBIgnore
    public Boolean hasShipyardData() {
        return ships.size() > 0;
    }

    @JsonIgnore
    @DynamoDBIgnore
    public Boolean hasOutfittingData() {
        return standardModules != null && hardpointModules != null && internalModules != null && utilityModules != null;
    }

    public void setSystemId(int systemId) {
        this.systemId = systemId;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public void setDistanceLs(Integer distanceLs) {
        this.distanceLs = distanceLs;
    }

    public void setPadSize(String padSize) {
        this.padSize = padSize;
    }

    public void setStationType(String stationType) {
        this.stationType = stationType;
    }

    public void setAllegiance(String allegiance) {
        this.allegiance = allegiance;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setHasShipyard(boolean hasShipyard) {
        this.hasShipyard = hasShipyard;
    }

    public void setHasOutfitting(boolean hasOutfitting) {
        this.hasOutfitting = hasOutfitting;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = DateTime.parse(lastUpdated);
    }

    public void setStandardModules(List<String> idList) throws UnknownIdException {
        this.standardModules = Modules.INSTANCE.standardFromIdList(idList);
    }

    public void setInternalModules(List<String> idList) throws UnknownIdException {
        this.internalModules = Modules.INSTANCE.internalFromIdList(idList);
    }

    public void setHardpointModules(List<String> idList) throws UnknownIdException {
        this.hardpointModules = Modules.INSTANCE.hardpointFromIdList(idList);
    }

    public void setUtilityModules(List<String> idList) throws UnknownIdException {
        this.utilityModules = Modules.INSTANCE.utilityFromIdList(idList);
    }

    public void setShips(List<String> ships) throws UnknownShipException {
        this.ships = Ship.from(ships);
    }

    public void setModules(ModuleSet standardModules, ModuleSet internalModules, ModuleSet hardpointModules, ModuleSet utilityModules) {
        this.standardModules = standardModules;
        this.internalModules = internalModules;
        this.hardpointModules = hardpointModules;
        this.utilityModules = utilityModules;
        refreshLastUpdated();
    }

    public void refreshLastUpdated() {
        this.lastUpdated = DateTime.now();
    }

    public boolean update(String stationName, int distanceLs, String allegiance, String padSize, String stationType, Boolean hasShipyard, Boolean hasOutfitting) {
        boolean updated = false;

        if (!stationName.equals(this.stationName)) {
            this.stationName = stationName;
            updated = true;
        }

        if (distanceLs != this.distanceLs) {
           this.distanceLs = distanceLs;
            updated = true;
        }
        if (!allegiance.equalsIgnoreCase(this.allegiance)) {
            this.allegiance = allegiance;
            updated = true;
        }
        if (!padSize.equalsIgnoreCase(this.padSize)) {
            this.padSize = padSize;
            updated = true;
        }
        if (!stationType.equalsIgnoreCase(this.stationType)) {
            this.stationType = stationType;
            updated = true;
        }
        if (this.hasShipyard != hasShipyard) {
            this.hasShipyard = hasShipyard;

            if (!hasShipyard) { // Remove ships
                ships = EnumSet.noneOf(Ship.class);
            }

            updated = true;
        }
        if (this.hasOutfitting != hasOutfitting) {
            this.hasOutfitting = hasOutfitting;

            if (!hasOutfitting) { // Remove Module data
                standardModules = null;
                internalModules = null;
                hardpointModules = null;
                utilityModules = null;
            }

            updated = true;
        }

        return updated;
    }
}
