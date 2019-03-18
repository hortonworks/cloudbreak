package com.sequenceiq.cloudbreak.domain;

public interface ArchivableResource {

    String getName();

    void setName(String name);

    void setArchived(boolean archived);
}
