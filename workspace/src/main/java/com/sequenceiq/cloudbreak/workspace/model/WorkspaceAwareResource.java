package com.sequenceiq.cloudbreak.workspace.model;


import org.springframework.security.access.AccessDeniedException;

public interface WorkspaceAwareResource extends TenantAwareResource {

    Long getId();

    Workspace getWorkspace();

    String getName();

    void setWorkspace(Workspace workspace);

    default String getResourceName() {
        return getClass().getSimpleName().toLowerCase();
    }

    @Override
    default Tenant getTenant() {
        if (getWorkspace() == null) {
            throw new AccessDeniedException(String.format("Workspace cannot be null for object: %s with name: %s",
                    getClass().toString(), (getName() == null) ? "name not provided" : getName()));
        }
        return getWorkspace().getTenant();
    }
}
