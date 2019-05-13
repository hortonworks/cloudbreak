package com.sequenceiq.cloudbreak.workspace.model;

public interface ArchivableResource {

    void setDeletionTimestamp(Long timestampMillisecs);

    void setArchived(boolean archived);

    void unsetRelationsToEntitiesToBeDeleted();
}
