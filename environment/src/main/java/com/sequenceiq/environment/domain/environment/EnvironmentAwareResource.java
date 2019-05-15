package com.sequenceiq.environment.domain.environment;

import java.util.Set;

import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

public interface EnvironmentAwareResource extends WorkspaceAwareResource {

    Set<EnvironmentView> getEnvironments();

    void setEnvironments(Set<EnvironmentView> environments);
}
