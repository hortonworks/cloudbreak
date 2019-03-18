package com.sequenceiq.cloudbreak.domain;

public interface ArchivableResource {

    void setDeletionTimestamp(Long timestampMillisecs);

    void setArchived(boolean archived);

    void unsetRelationsToEntitiesToBeDeleted();
}
