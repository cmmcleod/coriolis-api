package io.coriolis.api.resources;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.coriolis.api.core.StarSystem;
import io.coriolis.api.core.Station;
import io.coriolis.api.core.Universe;
import io.coriolis.api.resources.exceptions.JsonWebApplicationException;
import io.dropwizard.jersey.caching.CacheControl;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
@CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
public class SystemEndpoint {

    Universe universe;

    public SystemEndpoint(Universe universe) {
        this.universe = universe;
    }

    /**
     *
     * @param namePart
     * @return
     */
    @GET
    @Timed
    public Set<String> search(@QueryParam("str") String namePart) {
        if(namePart == null || namePart.length() < 3) {
            throw new JsonWebApplicationException("Query param str is required to be at least 3 characters long", Response.Status.BAD_REQUEST);
        }

        return universe.findSystemsWithName(namePart);
    }

    /**
     * Get the location and station details for a specific system
     * @param system The system name
     * @return details or throw 404 if not found
     */
    @GET
    @Timed
    @Path("/{systemName}")
    public StarSystem getSystem(@PathParam("systemName") String system) {
        StarSystem s =  universe.getSystem(system);

        if (s == null) {
            throw new JsonWebApplicationException("System does not exist or is not known", Response.Status.NOT_FOUND);
        }

        return s;
    }

    /**
     *
     * @param systemName
     * @param stationName
     * @return
     */
    @GET
    @Timed
    @Path("/{systemName}/{stationName}")
    public Station getStation(@PathParam("systemName") String systemName, @PathParam("stationName") String stationName) {
        StarSystem system =  universe.getSystem(systemName);

        if (system == null) {
            throw new JsonWebApplicationException("System does not exist or is not known", Response.Status.NOT_FOUND);
        }

        Station station = system.getStation(stationName);

        if (station == null) {
            throw new JsonWebApplicationException("Station in system '" + system.getSystemName() + "' does not exist or is not known", Response.Status.NOT_FOUND);
        }

        return station;
    }
}
