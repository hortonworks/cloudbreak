package com.sequenceiq.cloudbreak.common.archive;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public abstract class AbstractArchivistService<T extends ArchivableResource> {
    @Inject
    private Clock clock;

    public T delete(T resource) {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        MDCBuilder.buildMdcContext(resource);
        resource.setArchived(true);
        resource.setDeletionTimestamp(clock.getCurrentTimeMillis());
        resource.unsetRelationsToEntitiesToBeDeleted();
        repository().save(resource);
        MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        return resource;
    }

    public abstract JpaRepository<T, Long> repository();
}
