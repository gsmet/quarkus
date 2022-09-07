package io.quarkus.it.opentelemetry.reactive;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.mutiny.Uni;

@Path("/reactive")
public class ReactiveResource {
    @Inject
    Tracer tracer;

    @GET
    public Uni<String> helloGet(@QueryParam("name") String name) {
        Span span = tracer.spanBuilder("helloGet").startSpan();
        return Uni.createFrom().item("Hello " + name).onItem().delayIt().by(Duration.ofSeconds(2))
                .eventually((Runnable) span::end);
    }

    @POST
    public Uni<String> helloPost(String body) {
        Span span = tracer.spanBuilder("helloPost").startSpan();
        return Uni.createFrom().item("Hello " + body).onItem().delayIt().by(Duration.ofSeconds(2))
                .eventually((Runnable) span::end);
    }
}
