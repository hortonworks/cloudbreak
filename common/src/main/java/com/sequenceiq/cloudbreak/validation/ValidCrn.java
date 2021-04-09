package com.sequenceiq.cloudbreak.validation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;

@Documented
@Constraint(validatedBy = { CrnValidator.class, CrnCollectionValidator.class})
@Target({ METHOD, FIELD, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCrn {

    String message() default "Invalid Crn(s) Provided";

    CrnResourceDescriptor[] resource();

    Effect effect() default Effect.ACCEPT;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    enum Effect {
        ACCEPT("Accepted"),
        DENY("Denied");

        private String name;

        Effect(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
