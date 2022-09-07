package io.quarkus.it.rest

import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("")
interface CountriesClient {
    @POST
    @Path("/country")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun country(country: Country): Country

    @GET
    @Path("/countries")
    @Produces(MediaType.APPLICATION_JSON)
    fun countries(): List<Country>
}