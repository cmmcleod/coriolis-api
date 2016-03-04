package io.coriolis.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.coriolis.api.core.Ship;
import io.coriolis.api.core.modules.*;
import io.coriolis.api.core.modules.exceptions.UnknownIdException;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import org.joda.time.DateTime;


import java.util.*;

public class Station {

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


    public Station(int id, int systemId, String name, Integer distanceLs, String allegiance,
                   String padSize, String type, boolean hasShipyard, boolean hasOutfitting, List<String> ships, List<String> eddbModuleIds, DateTime lastUpdated) throws UnknownShipException {
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
        standardModules = null;
        internalModules = null;
        hardpointModules = null;
        utilityModules = null;

        if(hasShipyard && ships != null) {
            setShips(ships);
        } else {
            this.ships = null;
        }

        if (hasOutfitting && eddbModuleIds != null) {
            updateModulesFromEddbIdList(eddbModuleIds);
        }
    }

    public int getId() {
        return id;
    }

    public String getStationName() {
        return stationName;
    }

    public int getSystemId() {
        return systemId;
    }

    public Integer getDistanceLs() {
        return distanceLs;
    }

    @JsonProperty("ships")
    public List<String> getShips() {

        if(ships == null) {
            return null;
        }

        List<String> shipList = new ArrayList<>();
        for(Ship s : ships) {
            shipList.add(s.toString());
        }

        return shipList;
    }

    public String getStationType() {
        return stationType;
    }

    @JsonProperty("lastUpdated")
    public String getLastUpdated() {
        return lastUpdated.toString();
    }

    public String getAllegiance() {
        return allegiance;
    }

    public String getPadSize() {
        return padSize;
    }

    public boolean hasShip(Ship ship) {
        return ships != null && ships.contains(ship);
    }

    @JsonIgnore
    public ModuleSet getStandardSet() {
        return standardModules;
    }

    @JsonIgnore
    public ModuleSet getInternalSet() {
        return internalModules;
    }

    @JsonIgnore
    public ModuleSet getHardpointSet() {
        return hardpointModules;
    }

    @JsonIgnore
    public ModuleSet getUtilitySet() {
        return utilityModules;
    }

    @JsonProperty("standardModules")
    public List<String> getStandardModules() {
        if (standardModules != null) {
            return Modules.INSTANCE.toStandardList(standardModules);
        } else {
            return null;
        }
    }

    @JsonProperty("internalModules")
    public List<String> getInternalModules() {
        if (internalModules != null) {
            return Modules.INSTANCE.toInternalList(internalModules);
        } else {
            return null;
        }
    }

    @JsonProperty("hardpointModules")
    public List<String> getHardpointModules() {
        if (hardpointModules != null) {
            return Modules.INSTANCE.toHardPointList(hardpointModules);
        } else {
            return null;
        }
    }

    @JsonProperty("utilityModules")
    public List<String> getUtilityModules() {
        if (utilityModules != null) {
            return Modules.INSTANCE.toUtilityList(utilityModules);
        } else {
            return null;
        }
    }

    public boolean getHasShipyard() {
        return hasShipyard;
    }

    public boolean getHasOutfitting() {
        return hasOutfitting;
    }

    @JsonIgnore
    public Boolean hasShipyardData() {
        return ships != null;
    }

    @JsonIgnore
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

    private void updateModulesFromEddbIdList(List<String> eddbModuleIds) {
        Modules m = Modules.INSTANCE;
        int index;
        standardModules = m.createStandardSet();
        internalModules = m.createInternalSet();
        hardpointModules = m.createHardpointSet();
        utilityModules = m.createUtilitySet();

        for (String eddbId : eddbModuleIds) {
            index = m.getStandardIndexByEddbID(eddbId);
            if (index != -1) {
                standardModules.add(index);
                continue;
            }
            index = m.getInternalIndexByEddbID(eddbId);
            if (index != -1) {
                internalModules.add(index);
                continue;
            }
            index = m.getHardpointIndexByEddbID(eddbId);
            if (index != -1) {
                hardpointModules.add(index);
                continue;
            }
            index = m.getUtilityIndexByEddbID(eddbId);
            if (index != -1) {
                utilityModules.add(index);
            }
        }
    }

    public boolean update(String stationName,
                          int distanceLs,
                          String allegiance,
                          String padSize,
                          String stationType,
                          Boolean hasShipyard,
                          Boolean hasOutfitting,
                          List<String> ships,
                          List<String> eddbModuleIds) throws UnknownShipException {
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
                this.ships = EnumSet.noneOf(Ship.class);
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

        if(hasShipyard && ships != null) {
            setShips(ships);
            updated = true;
        }

        if (hasOutfitting && eddbModuleIds != null) {
            updateModulesFromEddbIdList(eddbModuleIds);
            updated = true;
        }

        return updated;
    }
}
