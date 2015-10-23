package io.coriolis.api.core;

import java.util.Comparator;

/**
 * Created as a seperate class for unit testing purposes
 */
public class BuildStationCandidateComparator implements Comparator<BuildStationCandidate> {

    @Override
    public int compare(BuildStationCandidate a, BuildStationCandidate b) {
        if(a.hasShip() && b.hasShip()) {
            if (a.getModulesFound() == b.getModulesFound()) {
                return a.getDistanceLY() == b.getDistanceLY() ? 0 : a.getDistanceLY() > b.getDistanceLY() ? -1 : 1;
            }
            return a.getModulesFound() < b.getModulesFound() ? -1 : 1;
        }

        return a.getScore() == b.getScore() ? 0 : a.getScore() < b.getScore() ? -1 : 1;
    }

}
