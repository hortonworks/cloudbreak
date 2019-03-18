package com.sequenceiq.cloudbreak.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.ArchivableResource;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public abstract class AbstractArchivistService<T extends WorkspaceAwareResource & ArchivableResource> extends AbstractWorkspaceAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractArchivistService.class);

    @Inject
    private Clock clock;

    @Override
    public T delete(T resource) {
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Archiving {} with name: {}", resource().getReadableName(), resource.getName());
        prepareDeletion(resource);
        resource.setArchived(true);
        resource.setDeletionTimestamp(clock.getCurrentTimeMillis());
        resource.unsetRelationsToEntitiesToBeDeleted();
        repository().save(resource);
        return resource;
    }
}
