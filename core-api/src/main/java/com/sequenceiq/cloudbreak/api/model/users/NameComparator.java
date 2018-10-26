package com.sequenceiq.cloudbreak.api.model.users;

import java.io.Serializable;
import java.util.Comparator;

public class NameComparator implements Comparator<WorkspaceResponse>, Serializable {

    @Override
    public int compare(WorkspaceResponse o1, WorkspaceResponse o2) {
        return o1.getName().compareTo(o2.getName());
    }
}
