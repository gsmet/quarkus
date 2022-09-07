package io.quarkus.context.test;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public String callMe() {
        return "Hello " + System.identityHashCode(this);
    }
}
