package com.sequenceiq.cloudbreak.service.decorator

/**
 * Decortor service interface for domain objects.
 * Implementers are expected to decorate passed in domain objects with data from other services.

 * @param  the type of the object to be decorated
 */
interface Decorator<T> {

    /**
     * Performs the decorator logic.

     * @param subject the object to be decorated
     * *
     * @param data    additional data
     * *
     * @return the decorated object
     */
    fun decorate(subject: T, vararg data: Any): T
}
