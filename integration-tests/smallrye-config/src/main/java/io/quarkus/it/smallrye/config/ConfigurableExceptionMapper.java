package io.quarkus.it.smallrye.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ConfigurableExceptionMapper
        implements ExceptionMapper<ConfigurableExceptionMapper.ConfigurableExceptionMapperException> {
    @Inject
    @ConfigProperty(name = "exception.message")
    String message;
    @Inject
    ExceptionConfig exceptionConfig;

    @Override
    public Response toResponse(final ConfigurableExceptionMapperException exception) {
        if (!message.equals(exceptionConfig.message())) {
            return Response.serverError().build();
        }

        return Response.ok().entity(message).build();
    }

    public static class ConfigurableExceptionMapperException extends RuntimeException {

    }
}
