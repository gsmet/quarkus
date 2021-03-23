package io.quarkus.hibernate.validator.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import io.quarkus.arc.DefaultBean;

@ApplicationScoped
public class ValidatorProducer {

    @Inject
    ValidatorFactory validatorFactory;

    @Produces
    @Singleton
    @DefaultBean
    public Validator validator() {
        return validatorFactory.getValidator();
    }
}
