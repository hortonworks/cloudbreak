package com.sequenceiq.cloudbreak.domain.workspace;


import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

public interface WorkspaceAwareResource extends TenantAwareResource {

    Long getId();

    Workspace getWorkspace();

    String getName();

    void setWorkspace(Workspace workspace);

    WorkspaceResource getResource();

    @Override
    default Tenant getTenant() {
        if (getWorkspace() == null) {
            throw new CloudbreakServiceException(String.format("Workspace cannot be null for object: %s with name: %s",
                    getClass().toString(), (getName() == null) ? "name not provided" : getName()));
        }
        return getWorkspace().getTenant();
    }
}
