package com.sequenceiq.cloudbreak.service;

import java.io.Serializable;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;

public abstract class ResourceArchivator<T extends ArchivableResource, I extends Serializable> {

    @Inject
    private Clock clock;

    public void archive(T resource) {
        resource.unsetRelationsToEntitiesToBeDeleted();
        resource.setArchived(true);
        resource.setDeletionTimestamp(clock.getCurrentTimeMillis());
        repository().save(resource);
    }

    protected abstract DisabledBaseRepository<T, Long> repository();

}
