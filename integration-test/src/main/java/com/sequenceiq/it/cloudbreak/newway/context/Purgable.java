package com.sequenceiq.it.cloudbreak.newway.context;

import java.util.Collection;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;

public interface Purgable<T> {

    Collection<T> getAll(CloudbreakClient client);

    boolean deletable(T entity);

    void delete(T entity, CloudbreakClient client);
}
