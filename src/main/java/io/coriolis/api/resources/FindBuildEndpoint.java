package io.coriolis.api.resources;


import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import io.coriolis.api.core.*;
import io.coriolis.api.core.modules.*;
import io.coriolis.api.core.modules.exceptions.UnknownIdException;
import io.coriolis.api.core.modules.exceptions.UnknownShipException;
import io.coriolis.api.resources.exceptions.JsonWebApplicationException;
import io.dropwizard.jersey.caching.CacheControl;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Path("/find")
@CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
@Produces(MediaType.APPLICATION_JSON)
public class FindBuildEndpoint {

    private Universe universe;
    private Modules modules;

    public FindBuildEndpoint(Universe universe) {
        this.universe = universe;
        this.modules = Modules.INSTANCE;
    }

    /**
     * Get the location and station details for a specific system
     * @param systemName The system name (404 if not found)
     * @return details
     */
    @GET
    @Timed
    @Path("/near/{systemName}/")
    public BuildStationCandidate[] findStations(@PathParam("systemName") String systemName,
                                                @QueryParam("ship") String shipName,
                                                @QueryParam("standard") String standard,
                                                @QueryParam("internal") String internal,
                                                @QueryParam("hardpoints") String hardpoints,
                                                @QueryParam("utilities") String utilities) {

        if (shipName == null && standard == null && internal == null && hardpoints == null && utilities == null) {
            throw new JsonWebApplicationException("Ship and/or modules are required query string parameters", Response.Status.BAD_REQUEST);
        }

        StarSystem system = universe.getSystem(systemName);

        if (system == null) {
            throw new JsonWebApplicationException("System does not exist or is not known", Response.Status.NOT_FOUND);
        }

        Ship ship = null;

        if (shipName != null) {
            try {
                ship = Ship.fromString(shipName.trim());
            } catch (UnknownShipException e) {
                throw new JsonWebApplicationException("Ship name '" + shipName + "' is invalid", Response.Status.BAD_REQUEST);
            }
        }

        ModuleMatcher s = null;

        if (!Strings.isNullOrEmpty(standard)) {
            try {
                s = new ModuleMatcher(modules.standardFromIdList(Arrays.asList(standard.split(","))));
            } catch (UnknownIdException e) {
                throw new JsonWebApplicationException("Unknown standard module: " + e.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        ModuleMatcher i = null;

        if (!Strings.isNullOrEmpty(internal)) {
            try {
                i = new ModuleMatcher(modules.internalFromIdList(Arrays.asList(internal.split(","))));
            } catch (UnknownIdException e) {
                throw new JsonWebApplicationException("Unknown internal module: " + e.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        ModuleMatcher h = null;

        if (!Strings.isNullOrEmpty(hardpoints)) {
            try {
                h = new ModuleMatcher(modules.hardpointFromIdList(Arrays.asList(hardpoints.split(","))));
            } catch (UnknownIdException e) {
                throw new JsonWebApplicationException("Unknown hardpoint module: " + e.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        ModuleMatcher u = null;

        if (!Strings.isNullOrEmpty(utilities)) {
            try {
                u = new ModuleMatcher(modules.utilityFromIdList(Arrays.asList(utilities.split(","))));
            } catch (UnknownIdException e) {
                throw new JsonWebApplicationException("Unknown utility module: " + e.getMessage(), Response.Status.BAD_REQUEST);
            }
        }

        return universe.findNear(system, ship, s, i, h, u);
    }
}
