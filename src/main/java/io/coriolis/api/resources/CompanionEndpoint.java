package io.coriolis.api.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Strings;
import io.coriolis.api.entities.CompanionSession;
import io.coriolis.api.core.frontier.CompanionClient;
import io.coriolis.api.resources.exceptions.JsonWebApplicationException;
import io.dropwizard.jersey.caching.CacheControl;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/companion")
@Produces(MediaType.APPLICATION_JSON)
@CacheControl(noCache = true, maxAge = 0)
public class CompanionEndpoint {

    CompanionClient companionClient;

    public CompanionEndpoint(HttpClient httpClient) {
        this.companionClient = new CompanionClient(httpClient);
    }

    @POST
    @Timed
    @Path("/login")
    public CompanionSession login(@FormParam("email") String email, @FormParam("password") String password) {
        if(Strings.isNullOrEmpty(email) || Strings.isNullOrEmpty(password)) {
            throw new JsonWebApplicationException("Email and Password required", Response.Status.BAD_REQUEST);
        }

        return companionClient.login(email, password);
    }

    @POST
    @Timed
    @Path("/confirm")
    public CompanionSession confirm(@Valid CompanionSession session) {
        return companionClient.confirm(session);
    }

    @POST
    @Timed
    @Path("/profile")
    public HttpEntity getProfile(@Valid CompanionSession session) {
       return companionClient.getProfile(session);
    }
}
