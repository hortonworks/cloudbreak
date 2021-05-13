package com.sequenceiq.cloudbreak.validation.customimage;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueRegionValidator.class)
public @interface UniqueRegion {

    String message() default "The region must be unique";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
