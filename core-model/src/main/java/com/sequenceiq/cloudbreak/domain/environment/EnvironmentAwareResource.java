package com.sequenceiq.cloudbreak.domain.environment;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

public interface EnvironmentAwareResource extends WorkspaceAwareResource {

    Set<EnvironmentView> getEnvironments();

    void setEnvironments(Set<EnvironmentView> environments);
}
