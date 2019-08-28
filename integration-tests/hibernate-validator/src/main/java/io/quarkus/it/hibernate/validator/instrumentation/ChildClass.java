package io.quarkus.it.hibernate.validator.instrumentation;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ChildClass extends ParentClass {

    @NotNull
    public Long longObject;

    @Min(4)
    public long primitiveLong;

    private String string;

    public static ChildClass valid() {
        return new ChildClass(4L, 6l, "string", "hello");
    }

    public static ChildClass invalid() {
        return new ChildClass(null, 2l, "", "");
    }

    private ChildClass(Long longObject, long primitiveLong, String string, String grandParentString) {
        super(grandParentString);
        this.longObject = longObject;
        this.primitiveLong = primitiveLong;
        this.string = string;
    }

    @NotBlank
    public String getString() {
        return string;
    }

    @Min(5)
    public long getPrimitiveLong() {
        return primitiveLong;
    }
}
