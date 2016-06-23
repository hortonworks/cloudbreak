package com.sequenceiq.cloudbreak.cloud.service

/**
 * Interface to be implemented by the persistence provider module.
 * (It's intended to be injected as collaborator where persisted data access is
 * required)
 *
 *
 * Implementers are required to provide the specific conversion logic between
 * the generic type and persisted data.

 * @param  the type of the (wrapped) data to be persisted.
 */
interface Persister<T> {

    fun persist(data: T): T

    fun update(data: T): T

    fun retrieve(data: T): T

    fun delete(notification: T): T
}
