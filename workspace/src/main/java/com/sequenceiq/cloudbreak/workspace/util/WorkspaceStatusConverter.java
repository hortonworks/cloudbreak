package com.sequenceiq.cloudbreak.workspace.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus;

public class WorkspaceStatusConverter extends DefaultEnumConverter<WorkspaceStatus> {

    @Override
    public WorkspaceStatus getDefault() {
        return WorkspaceStatus.ACTIVE;
    }
}
