package com.sequenceiq.cloudbreak.service.decorator;

/**
 * Decortor service interface for domain objects.
 * Implementers are expected to decorate passed in domain objects with data from other services.
 *
 * @param <T> the type of the object to be decorated
 */
public interface Decorator<T> {

    /**
     * Performs the decorator logic.
     *
     * @param subject the object to be decorated
     * @param data    additional data
     * @return the decorated object
     */
    T decorate(T subject, Object... data);
}
