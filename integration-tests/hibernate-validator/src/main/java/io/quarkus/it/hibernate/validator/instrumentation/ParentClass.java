package io.quarkus.it.hibernate.validator.instrumentation;

public class ParentClass extends GrandParentClass {

    // this class does not have any constrained property but we need it to be instrumented anyway.

    public ParentClass(String grandParentString) {
        super(grandParentString);
    }
}
