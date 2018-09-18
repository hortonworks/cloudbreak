package com.sequenceiq.cloudbreak.controller;

import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;

public interface WorkspaceAwareResourceController<T extends WorkspaceAwareResource> {

    Class<? extends WorkspaceResourceRepository<T, ?>> getWorkspaceAwareResourceRepository();

}
