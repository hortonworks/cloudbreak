package com.sequenceiq.it.cloudbreak.context;

import java.util.Collection;

import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;

public interface Purgable<T, U extends MicroserviceClient> extends Orderable {

    Collection<T> getAll(U client);

    boolean deletable(T entity);

    void delete(TestContext testContext, T entity, U client);

    Class<U> client();

}
