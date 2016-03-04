package io.coriolis.api.core;

import cern.colt.matrix.impl.SparseObjectMatrix3D;
import com.BoxOfC.MDAG.MDAG;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.coriolis.api.core.modules.*;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import io.coriolis.api.entities.BuildStationCandidate;
import io.coriolis.api.entities.StarSystem;
import io.coriolis.api.entities.Station;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class Universe {

    final static Logger logger = LoggerFactory.getLogger(Universe.class);

    public static final int INHABITED_RANGE_LY = 2000; // Conservative Max Distance from Sol * 2 (current max is Sothis 494.49 ly)
    public static final int SECTOR_SIZE_LY = 100;  // 100 Light years cubed
    public static final int SECTOR_RANGE = INHABITED_RANGE_LY / SECTOR_SIZE_LY; // Max number of sectors in a dimension
    public static final int SECTOR_MAX = SECTOR_RANGE / 2;
    public static final int SECTOR_MIN = SECTOR_MAX * -1;
    public static final int SEARCH_SURROUNDING_SECTORS = 5;   // Search area (cube of sectors of side size N)
    public static final int MAX_RESULTS = 15;
    public static final BuildStationCandidateComparator bscComparator = new BuildStationCandidateComparator();

    private Map<String, StarSystem> systemsNameMap;
    private Map<Integer, StarSystem> systemsIdMap;
    private MDAG systemNamesMDAG;
    private SparseObjectMatrix3D sectors;

    private Counter unknownSystems;
    private Counter unknownStations;
    private Meter stationUpdates;
    private Counter stationCounter;
    private Counter stationWithOutfitting;
    private Counter stationWithShipyard;
    private Counter stationHasOutfittingData;
    private Counter stationHasShipyardData;
    private Counter systemCounter;

    public Universe(MetricRegistry metrics) {
        systemsNameMap = new HashMap<>();
        systemsIdMap = new HashMap<>();
        sectors = new SparseObjectMatrix3D(SECTOR_RANGE, SECTOR_RANGE, SECTOR_RANGE);
        unknownSystems = metrics.counter("unknownSystems");
        unknownStations = metrics.counter("unknownStations");
        stationUpdates = metrics.meter("stationUpdates");
        stationCounter = metrics.counter("stations");
        systemCounter = metrics.counter("systems");
        stationWithOutfitting = metrics.counter("stationWithOutfitting");
        stationWithShipyard = metrics.counter("stationWithShipyard");
        stationHasOutfittingData = metrics.counter("stationHasOutfittingData");
        stationHasShipyardData = metrics.counter("stationHasShipyardData");
        systemNamesMDAG = new MDAG(new ArrayList<String>());
    }

    public StarSystem getSystem(String systemName) {
        return systemsNameMap.get(systemName.toLowerCase());
    }

    public StarSystem getSystem(int id) {
        return systemsIdMap.get(id);
    }

    public Set<String> findSystemsWithName(String namePart) {
        Set<String> systemsWithName = new HashSet<>();

        for(String s : systemNamesMDAG.getStringsWithSubstring(namePart.toLowerCase())) {
            systemsWithName.add(getSystem(s).getSystemName()); // Return proper name case
        }
        return systemsWithName;
    }

    public BuildStationCandidate[] findNear(StarSystem current,
                                            Ship ship,
                                            ModuleMatcher standardMatcher,
                                            ModuleMatcher internalMatcher,
                                            ModuleMatcher hardpointMatcher,
                                            ModuleMatcher utilityMatcher) {
        PriorityQueue<BuildStationCandidate> candidates = new PriorityQueue<>(20, bscComparator);
        int oX = current.getSectorX();
        int oY = current.getSectorY();
        int oZ = current.getSectorZ();
        int sectorRadius = 0;
        double moduleCount = (standardMatcher != null ? standardMatcher.count() : 0)
                + (internalMatcher != null ? internalMatcher.count() : 0)
                + (hardpointMatcher != null ? hardpointMatcher.count() : 0)
                + (utilityMatcher != null ? utilityMatcher.count() : 0);

        boolean buildFound = false;

        while (sectorRadius < SEARCH_SURROUNDING_SECTORS && !buildFound) {
            int sectorMin = sectorRadius * -1;

            for (int x = sectorMin; x <= sectorRadius; x++ ) {
                for (int y = sectorMin; y <= sectorRadius; y++ ) {
                    for (int z = sectorMin; z <= sectorRadius; z++ ) {
                        if(x != sectorMin || x != sectorRadius || y != sectorMin || y != sectorRadius || z != sectorMin || z != sectorRadius) {
                            // Do not search sectors inside the radius as they will already have been searched
                            // This can probably be improved
                            continue;
                        }

                        List<StarSystem> sector = getSector(oX + x, oY + y, oZ + z);

                        if (sector == null) {
                            continue;
                        }

                        for (StarSystem system : sector) {
                            for (Station station : system.getStations()) {
                                int modulesFound = 0;
                                double score = 0;
                                boolean hasShip = ship == null ? false : station.hasShip(ship);

                                if (moduleCount > 0) {
                                    if (standardMatcher != null) {
                                        modulesFound += standardMatcher.match(station.getStandardSet());
                                    }
                                    if (internalMatcher != null) {
                                        modulesFound += internalMatcher.match(station.getInternalSet());
                                    }
                                    if (hardpointMatcher != null) {
                                        modulesFound += hardpointMatcher.match(station.getHardpointSet());
                                    }
                                    if (utilityMatcher != null) {
                                        modulesFound += utilityMatcher.match(station.getUtilitySet());
                                    }
                                    score = modulesFound / moduleCount;
                                }

                                if (hasShip) {
                                    score += 1;
                                }

                                if (score > 0) {
                                    candidates.offer(new BuildStationCandidate(system.getSystemName(), station, hasShip, score, modulesFound, system.lightYearsFrom(current)));
                                    if (score == 2) {
                                        // All components and Ship found but continue searching for other options with in the current radius
                                        buildFound = true;
                                    }
                                }
                                if (candidates.size() >= MAX_RESULTS) { // Trim search results
                                    candidates.poll();
                                }
                            }
                        }
                    }
                }
            }
            sectorRadius++;
        }

        return candidates.toArray(new BuildStationCandidate[candidates.size()]);
    }

    public void loadSystem(StarSystem system) {
        systemsIdMap.put(system.getId(), system);
        systemsNameMap.put(system.getSystemName().toLowerCase(), system);
        systemNamesMDAG.addString(system.getSystemName().toLowerCase());
        addSystemToSector(system);
        systemCounter.inc();
    }

    public StarSystem updateSystemFromEDDB(int id, String systemName, double x, double y, double z, boolean needsPermit) {
        // Known / Existing Star System
        if (systemsIdMap.containsKey(id)) {
            StarSystem existingSystem = systemsIdMap.get(id);
            boolean updated = false;
            if(!systemName.equals(existingSystem.getSystemName())) {        // System name changes (case sensitive)
                systemNamesMDAG.removeString(existingSystem.getSystemName());
                systemNamesMDAG.addString(systemName.toLowerCase());
                existingSystem.setSystemName(systemName);
                updated = true;
            }
            if(existingSystem.getNeedsPermit() != needsPermit) {    // Permit required changed
                existingSystem.setNeedsPermit(needsPermit);
                updated = true;
            }
            if(!existingSystem.hasCoordinates(x, y, z)) {   // System coordinates need updating
                if (!existingSystem.isInSectorWithCoordinates(x, y, z)) {  // Sector needs updating
                    getSectorFor(existingSystem).remove(existingSystem);    // Remove from current sector
                    existingSystem.setCoordinates(x, y, z);  // Update existing system coordinates
                    addSystemToSector(existingSystem);  // Add to corrected sector
                }
                existingSystem.setCoordinates(x, y, z);
                updated = true;
            }
            return updated ? existingSystem : null;
        // Previously unknown Star System
        } else if (areCoordinatesInRange(x, y, z)) {
            StarSystem system = new StarSystem(id, systemName, x, y, z, needsPermit);
            loadSystem(system);
            return system;
        } else {    // System outside of Universe bubble set by INHABITED_RANGE_LY / 2
            //double dist = lightYearsBetween(0, 0, 0, x, y, z);
            //logger.warn("System outside of range:" + systemName + " [" + id + "] - " + dist + "LY");
            return null;
        }
    }

    public void loadStation(StarSystem system, Station station) {
        system.add(station);
        stationCounter.inc();
        if(station.getHasOutfitting()) {
            stationWithOutfitting.inc();
            if(station.hasOutfittingData()) {
                stationHasOutfittingData.inc();
            }
        }
        if(station.getHasShipyard()) {
            stationWithShipyard.inc();
            if (station.hasShipyardData()) {
                stationHasShipyardData.inc();
            }
        }
    }

    public Station updateStationFromEDDB(StarSystem system,
                                         int stationId,
                                         String stationName,
                                         int distanceLs,
                                         String allegiance,
                                         String padSize,
                                         String type,
                                         Boolean shipyard,
                                         Boolean outfitting,
                                         List<String> ships,
                                         List<String> eddbModuleIds) throws UnknownShipException {
        Station existingStation = system.getStation(stationId);

        if (existingStation == null) {
            Station station = new Station(stationId, system.getId(), stationName, distanceLs, allegiance, padSize, type, shipyard, outfitting, ships, eddbModuleIds, DateTime.now());
            loadStation(system, station);
            return station;
        } else {
            boolean hadShipyardBefore = existingStation.getHasShipyard();
            boolean hadOutfittingBefore = existingStation.getHasOutfitting();
            if (existingStation.update(stationName, distanceLs, allegiance, padSize, type, shipyard, outfitting, ships, eddbModuleIds)) {
                stationUpdates.mark();

                if(!hadShipyardBefore && existingStation.getHasShipyard()) { // Shipyard added
                    stationWithShipyard.inc();
                } else if(hadShipyardBefore && !existingStation.getHasShipyard()) { // Shipyard removed
                    stationWithShipyard.dec();
                }

                if(!hadOutfittingBefore && existingStation.getHasOutfitting()) { // Outfitting added
                    stationWithOutfitting.inc();
                } else if(hadOutfittingBefore && !existingStation.getHasOutfitting()) { // Outfitting removed
                    stationWithOutfitting.dec();
                }

                return existingStation;
            }
        }
        return null;
    }

    public void updateStationFromEDDN(String systemName, String stationName, List<String> ships) throws UnknownShipException {
        StarSystem system = getSystem(systemName);
        if (system != null) {
            Station existingStation = system.getStation(stationName);

            if (existingStation == null) {
                unknownStations.inc();
                logger.warn("Unknown Station: " + systemName + " [" + system.getId() + "] - " + stationName);
            } else {
                boolean hadShipyardBefore = existingStation.getHasShipyard();
                boolean hadShipyardDataBefore = existingStation.hasShipyardData();
                logger.debug("Updating Shipyard for Station: " + systemName + " [" + system.getId() + "] - " + stationName);
                existingStation.setShips(ships);
                stationUpdates.mark();

                if (!hadShipyardDataBefore) {
                    stationHasShipyardData.inc();
                    if (!hadShipyardBefore) {
                        existingStation.setHasShipyard(true);
                        stationWithShipyard.inc();
                    }
                }

            }
        } else {
            unknownSystems.inc();
            logger.warn("Unknown system " + systemName + " for station " + stationName);
        }
    }

    public void updateStationFromEDDN(String systemName, String stationName, ModuleSet s, ModuleSet i, ModuleSet h, ModuleSet u) {
        StarSystem system = getSystem(systemName);
        if (system != null) {
            Station existingStation = system.getStation(stationName);

            if (existingStation == null) {
                unknownStations.inc();
                logger.warn("Unknown Station: " + systemName + " [" + system.getId() +  "] - "  + stationName);
            } else {
                logger.debug("Updating Outfitting for Station: " + systemName + " [" + system.getId() +  "] - "  + stationName);
                boolean hadOutfittingBefore = existingStation.getHasOutfitting();
                boolean hadOutfittingDataBefore = existingStation.hasOutfittingData();
                existingStation.setModules(s, i, h, u);
                stationUpdates.mark();

                if(!hadOutfittingDataBefore) {
                    stationHasOutfittingData.inc();
                    if(!hadOutfittingBefore) {
                        existingStation.setHasOutfitting(true);
                        stationWithOutfitting.inc();
                    }
                }
            }
        } else {
            unknownSystems.inc();
            logger.warn("Unknown system " + systemName);
        }
    }

    /* public static methods */

    public static boolean areCoordinatesInRange(double x, double y, double z) {
        return isSectorInRange((int) Math.round(x / SECTOR_SIZE_LY), (int) Math.round(y / SECTOR_SIZE_LY), (int) Math.round(z / SECTOR_SIZE_LY));
    }

    public static boolean isSectorInRange(int x, int y, int z) {
        return x >= SECTOR_MIN && x <= SECTOR_MAX && y >= SECTOR_MIN && y <= SECTOR_MAX && z >= SECTOR_MIN && z <= SECTOR_MAX;
    }

    public static int coordinateToSector(double dim) {
        return (int) (Math.round(dim) / SECTOR_SIZE_LY) + SECTOR_MIN;
    }

    public static double lightYearsBetween(double oX, double oY, double oZ, double eX, double eY, double eZ) {
        return Math.sqrt( Math.pow(oX - eX, 2) + Math.pow(oY - eY, 2) + Math.pow(oZ - eZ, 2) );
    }

    /* private methods */

    private List<StarSystem> getSectorFor(StarSystem system) {
        return getSector(system.getSectorX(), system.getSectorY(), system.getSectorZ());
    }

    private List<StarSystem> getSector(int x, int y, int z) {
        Object sectorList = sectors.getQuick(x, y, z);
        if(sectorList == null) {    // Sector is empty
            return null;
        } else {
            return  (ArrayList<StarSystem>) sectorList;
        }
    }

    private void addSystemToSector(StarSystem system) {
        List<StarSystem> sectorList;

        Object sectorObj = sectors.getQuick(system.getSectorX(), system.getSectorY(), system.getSectorZ());
        if(sectorObj == null) {    // Sector is empty
             sectorList =  new ArrayList<>();
            sectors.setQuick(system.getSectorX(), system.getSectorY(), system.getSectorZ(), sectorList);
        } else {
            sectorList = (ArrayList<StarSystem>) sectorObj;
        }
        sectorList.add(system);
    }
}
