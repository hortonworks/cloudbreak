package com.sequenceiq.cloudbreak.structuredevent.rest.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to associate a JPA entity with a {@code @Path} annotation.
 *
 * This should be used on a Controller class that extends an interface annotated with {@code @Path}.
 *
 * The value should be a JPA {@code @Entity}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface AccountEntityType {

    Class<?> value();
}
