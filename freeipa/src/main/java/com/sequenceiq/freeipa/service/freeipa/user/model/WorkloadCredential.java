package com.sequenceiq.freeipa.service.freeipa.user.model;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class WorkloadCredential {

    private final String hashedPassword;

    private final ImmutableList<ActorKerberosKey> keys;

    public WorkloadCredential(String hashedPassword, List<ActorKerberosKey> keys) {
        this.hashedPassword = hashedPassword;
        this.keys = ImmutableList.copyOf(keys);
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public List<ActorKerberosKey> getKeys() {
        return keys;
    }

}
