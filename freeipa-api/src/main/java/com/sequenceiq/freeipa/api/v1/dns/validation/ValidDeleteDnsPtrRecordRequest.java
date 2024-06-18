package com.sequenceiq.freeipa.api.v1.dns.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DeleteDnsPtrRecordRequestValidator.class)
public @interface ValidDeleteDnsPtrRecordRequest {
    String message() default "Invalid delete dns ptr record request";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
