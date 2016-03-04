package io.coriolis.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.coriolis.api.core.Universe;

import java.util.HashSet;
import java.util.Set;

public class StarSystem {

    private int id;
    private String systemName;
    private Set<Station> stations;
    private boolean needsPermit;
    private double x;
    private double y;
    private double z;

    @JsonIgnore
    private int sectorX;
    @JsonIgnore
    private int sectorY;
    @JsonIgnore
    private int sectorZ;

    public StarSystem(int id, String name, double x, double y, double z, boolean needsPermit) {
        this.id = id;
        this.systemName = name;
        this.stations = new HashSet<>();
        this.needsPermit = needsPermit;
        setCoordinates(x, y, z);
    }

    public int getId() {
        return id;
    }

    public String getSystemName() {
        return systemName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean getNeedsPermit(){
        return needsPermit;
    }

    public Set<Station> getStations() {
        return stations;
    }

    public Station getStation(String stationName) {
        for(Station s : stations) {
            if(s.getStationName().equalsIgnoreCase(stationName)) {
                return s;
            }
        }
        return null;
    }

    public Station getStation(int stationId) {
        for(Station s : stations) {
            if(s.getId() == stationId) {
                return s;
            }
        }
        return null;
    }

    public int getSectorX() {
        return sectorX;
    }

    public int getSectorY() {
        return sectorY;
    }

    public int getSectorZ() {
        return sectorZ;
    }

    public boolean hasCoordinates(double x, double y, double z) {
        return this.x == x && this.y == y && this.z == z;
    }

    public boolean isInSectorWithCoordinates(double x, double y, double z) {
        return sectorX == Universe.coordinateToSector(x) && sectorY == Universe.coordinateToSector(y) && sectorZ == Universe.coordinateToSector(z);
    }

    public double lightYearsFrom(StarSystem system) {
        return Universe.lightYearsBetween(this.x, this.y, this.z, system.getX(), system.getY(), system.getZ());
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public void setStations(Set<Station> stations) {
        this.stations = stations;
    }

    public void setNeedsPermit(boolean needsPermit) {
        this.needsPermit = needsPermit;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setCoordinates(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        sectorX = Universe.coordinateToSector(x);
        sectorY = Universe.coordinateToSector(y);
        sectorZ = Universe.coordinateToSector(z);
    }

    public void add(Station station) {
        stations.add(station);
    }



}
