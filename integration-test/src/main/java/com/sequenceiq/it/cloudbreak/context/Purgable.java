package com.sequenceiq.it.cloudbreak.context;

import java.util.Collection;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;

public interface Purgable<T> extends Orderable {

    Collection<T> getAll(CloudbreakClient client);

    boolean deletable(T entity);

    void delete(TestContext testContext, T entity, CloudbreakClient client);
}
