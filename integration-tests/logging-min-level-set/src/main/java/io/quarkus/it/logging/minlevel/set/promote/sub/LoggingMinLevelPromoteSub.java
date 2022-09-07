package io.quarkus.it.logging.minlevel.set.promote.sub;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

import io.quarkus.it.logging.minlevel.set.LoggingWitness;

@Path("/log/promote/sub")
public class LoggingMinLevelPromoteSub {

    static final Logger LOG = Logger.getLogger(LoggingMinLevelPromoteSub.class);

    @GET
    @Path("/not-info")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isNotInfo() {
        return !LOG.isInfoEnabled() && !LoggingWitness.loggedInfo("should not print", LOG);
    }

    @GET
    @Path("/error")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isError() {
        return LoggingWitness.loggedError("error message", LOG);
    }

}
