package com.sequenceiq.cloudbreak.common.archive;

public interface ArchivableResource {

    void setDeletionTimestamp(Long timestampMillisecs);

    void setArchived(boolean archived);

    void unsetRelationsToEntitiesToBeDeleted();
}
