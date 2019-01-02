package com.sequenceiq.cloudbreak.api.endpoint.v4.workspace;

import java.io.Serializable;
import java.util.Comparator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceV4Response;

public class NameComparator implements Comparator<WorkspaceV4Response>, Serializable {

    @Override
    public int compare(WorkspaceV4Response o1, WorkspaceV4Response o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
