package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public interface StackWorkspaceView {

    Long getId();

    String getOwner();

    Workspace getWorkspace();
}
