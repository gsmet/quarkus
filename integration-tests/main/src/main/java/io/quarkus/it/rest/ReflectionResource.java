package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/reflection")
public class ReflectionResource {

    @GET
    @Path("/simpleClassName")
    public String getSimpleClassName(@QueryParam("className") String className) {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            return clazz.getSimpleName();
        } catch (ClassNotFoundException e) {
            return "FAILED";
        }
    }
}
