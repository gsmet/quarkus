package org.acme;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

@Path("/hello")
public class HelloResource {

    final CommonBean common;
    final LibraryBean library;

    @Inject
    @ConfigProperty(name = "greeting")
    String greeting;

    public HelloResource(CommonBean common, LibraryBean library) {
        this.common = java.util.Objects.requireNonNull(common);
        this.library = java.util.Objects.requireNonNull(library);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.TEXT_PLAIN)
    public String greeting() {
        return greeting;
    }
}
