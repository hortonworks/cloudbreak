package com.sequenceiq.cloudbreak.domain.workspace;


import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;

public interface WorkspaceAwareResource extends TenantAwareResource {

    Long getId();

    Workspace getWorkspace();

    String getName();

    void setWorkspace(Workspace workspace);

    WorkspaceResource getResource();

    @Override
    default Tenant getTenant() {
        return getWorkspace().getTenant();
    }
}
