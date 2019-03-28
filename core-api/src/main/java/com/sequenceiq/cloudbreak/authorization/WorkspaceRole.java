package com.sequenceiq.cloudbreak.authorization;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public enum WorkspaceRole {
    WORKSPACEREADER("WorkspaceReader"),
    WORKSPACEWRITER("WorkspaceWriter"),
    WORKSPACEMANAGER("WorkspaceManager");

    private String umsName;

    WorkspaceRole(String umsName) {
        this.umsName = umsName;
    }

    public String getUmsName() {
        return umsName;
    }

    public static WorkspaceRole getByUmsName(String umsName) {
        return Arrays.stream(values()).filter(workspaceRole -> StringUtils.equals(workspaceRole.umsName, umsName)).findFirst().get();
    }

    public String getCrn(String actorCrn) {
        return Crn.builder()
                .setAccountId(Crn.fromString(actorCrn).getAccountId())
                .setResource(getUmsName())
                .setResourceType(Crn.ResourceType.RESOURCE_ROLE)
                .setService(Crn.fromString(actorCrn).getService())
                .build()
                .toString();
    }
}
