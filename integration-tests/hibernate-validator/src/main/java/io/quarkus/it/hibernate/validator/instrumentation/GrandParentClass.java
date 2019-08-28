package io.quarkus.it.hibernate.validator.instrumentation;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class GrandParentClass {

    @NotBlank
    public String grandParentString;

    GrandParentClass(String grandParentString) {
        this.grandParentString = grandParentString;
    }

    @Pattern(regexp = "hello")
    public String getGrandParentString() {
        return grandParentString;
    }
}
