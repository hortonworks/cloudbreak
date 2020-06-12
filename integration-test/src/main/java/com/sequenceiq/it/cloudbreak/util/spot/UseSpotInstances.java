package com.sequenceiq.it.cloudbreak.util.spot;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface UseSpotInstances {
}
