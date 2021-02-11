package com.sequenceiq.freeipa.service.freeipa.user.model;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import org.apache.commons.lang3.StringUtils;

public class WorkloadCredentialUpdate {

    private final String username;

    private final String userCrn;

    private final WorkloadCredential workloadCredential;

    public WorkloadCredentialUpdate(String username, String userCrn, WorkloadCredential workloadCredential) {
        checkArgument(StringUtils.isNotBlank(username), "username must not be blank");
        checkArgument(StringUtils.isNotBlank(userCrn), "user CRN must not be blank");
        this.workloadCredential = requireNonNull(workloadCredential, "workload credential must not be null");
        this.username = username;
        this.userCrn = userCrn;
    }

    public String getUsername() {
        return username;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public WorkloadCredential getWorkloadCredential() {
        return workloadCredential;
    }
}
