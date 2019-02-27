package com.sequenceiq.it.cloudbreak.newway.context;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({METHOD, PARAMETER})
public @interface Description {

    String given() default "";

    String when() default "";

    String then() default "";

}
