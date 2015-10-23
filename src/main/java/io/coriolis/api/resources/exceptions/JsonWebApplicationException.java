package io.coriolis.api.resources.exceptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by cmmcleod on 10/15/15.
 */
public class JsonWebApplicationException extends WebApplicationException {

    /**
     * Create a HTTP 409 (Conflict) exception.
     * @param message the String that is the exception message of the 409 response.
     */
    public JsonWebApplicationException(String message, Response.Status status) {
        super(Response.status(status).
                entity(new JSONExceptionMessageContainer(message)).type(MediaType.APPLICATION_JSON).build());
    }

}
