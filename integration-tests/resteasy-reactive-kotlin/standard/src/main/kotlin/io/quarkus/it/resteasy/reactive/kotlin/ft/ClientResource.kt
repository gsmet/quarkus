package io.quarkus.it.resteasy.reactive.kotlin.ft

import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.concurrent.atomic.AtomicBoolean
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path

@Path("/ft/client")
class ClientResource {
    @Inject
    @RestClient
    private lateinit var client: HelloClient

    @GET
    suspend fun get() = client.hello()
}
