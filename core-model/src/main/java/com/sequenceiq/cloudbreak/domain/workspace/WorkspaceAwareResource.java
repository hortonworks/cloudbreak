package com.sequenceiq.cloudbreak.domain.workspace;


import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;

public interface WorkspaceAwareResource {

    Long getId();

    Workspace getWorkspace();

    String getName();

    void setWorkspace(Workspace workspace);

    WorkspaceResource getResource();

    String getOwner();
}
