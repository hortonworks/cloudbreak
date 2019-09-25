package com.sequenceiq.freeipa.service.freeipa.user.model;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;

import java.util.List;

public class WorkloadCredential {

    private String hashedPassword;

    private List<ActorKerberosKey> keys;

    public WorkloadCredential(String hashedPassword, List<ActorKerberosKey> keys) {
        this.hashedPassword = hashedPassword;
        this.keys = keys;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public List<ActorKerberosKey> getKeys() {
        return keys;
    }

}
