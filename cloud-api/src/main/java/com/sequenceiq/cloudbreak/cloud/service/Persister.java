package com.sequenceiq.cloudbreak.cloud.service;

/**
 * Interface to be implemented by the persistence provider module.
 * (It's intended to be injected as collaborator where persisted data access is
 * required)
 * <p/>
 * Implementers are required to provide the specific conversion logic between
 * the generic type and persisted data.
 *
 * @param <T> the type of the (wrapped) data to be persisted.
 */
public interface Persister<T> {

    T persist(T data);

    T retrieve(T data);

}
