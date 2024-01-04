package com.sequenceiq.cloudbreak.service;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

public abstract class AbstractArchivistService<T extends WorkspaceAwareResource & ArchivableResource> extends AbstractWorkspaceAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractArchivistService.class);

    @Inject
    private Clock clock;

    @Override
    public T delete(T resource) {
        Map<String, String> mdcContextMap = MDCBuilder.getMdcContextMap();
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Archiving {} with name: {}", resource.getResourceName(), resource.getName());
        prepareDeletion(resource);
        resource.setArchived(true);
        resource.setDeletionTimestamp(clock.getCurrentTimeMillis());
        resource.unsetRelationsToEntitiesToBeDeleted();
        repository().save(resource);
        MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        return resource;
    }
}
