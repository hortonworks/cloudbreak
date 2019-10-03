package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public interface StackTtlView {

    Long getId();

    String getName();

    String getCrn();

    Workspace getWorkspace();

    StackStatus getStatus();

    Long getCreationFinished();
}
