package com.sequenceiq.cloudbreak.common.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface for @JsonCreator constructor parameters that are explicitly ignored from deserialization
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface JsonIgnoreDeserialization {

}
