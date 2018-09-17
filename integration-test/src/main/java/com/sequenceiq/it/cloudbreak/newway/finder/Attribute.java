package com.sequenceiq.it.cloudbreak.newway.finder;


import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;

public interface Attribute<T extends CloudbreakEntity, O> {
    O get(T entity);
}

