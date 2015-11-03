package io.coriolis.api.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.coriolis.api.entities.Station;

public class BuildStationCandidate {

    @JsonProperty
    private Station station;
    @JsonProperty
    private String systemName;
    private boolean hasShip;
    @JsonProperty
    private double distanceLY;
    @JsonProperty
    private double score;
    @JsonProperty
    private int modulesFound;

    public BuildStationCandidate(String systemName, Station station, boolean hasShip, double score, int modulesFound, double distanceLY) {
        this.systemName = systemName;
        this.station = station;
        this.hasShip = hasShip;
        this.score = score;
        this.modulesFound = modulesFound;
        this.distanceLY = Math.round(distanceLY * 100.0) / 100.0;
    }

    public String getSystemName() {
        return systemName;
    }

    public Station getStation() {
        return station;
    }

    @JsonProperty("hasShip")
    public boolean hasShip() {
        return hasShip;
    }

    public double getDistanceLY() {
        return distanceLY;
    }

    public double getScore() {
        return score;
    }

    public int getModulesFound() {
        return modulesFound;
    }

}
