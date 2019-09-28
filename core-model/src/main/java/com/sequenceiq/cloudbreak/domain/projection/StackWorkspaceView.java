package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

public interface StackWorkspaceView {

    Long getId();

    String getOwner();

    Workspace getWorkspace();
}
