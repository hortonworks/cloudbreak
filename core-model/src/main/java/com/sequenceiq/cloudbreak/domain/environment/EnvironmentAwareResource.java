package com.sequenceiq.cloudbreak.domain.environment;

import java.util.Set;

import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;

public interface EnvironmentAwareResource extends WorkspaceAwareResource {

    Set<EnvironmentView> getEnvironments();

    void setEnvironments(Set<EnvironmentView> environments);
}
