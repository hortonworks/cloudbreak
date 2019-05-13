package com.sequenceiq.cloudbreak.workspace.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.workspace.model.ArchivableResource;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

public abstract class AbstractArchivistService<T extends WorkspaceAwareResource & ArchivableResource> extends AbstractWorkspaceAwareResourceService<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractArchivistService.class);

    @Override
    public T delete(T resource) {
        MDCBuilder.buildMdcContext(resource);
        LOGGER.debug("Archiving {} with name: {}", resource().getReadableName(), resource.getName());
        prepareDeletion(resource);
        resource.setArchived(true);
        resource.setDeletionTimestamp(Instant.now().toEpochMilli());
        resource.unsetRelationsToEntitiesToBeDeleted();
        repository().save(resource);
        return resource;
    }
}
