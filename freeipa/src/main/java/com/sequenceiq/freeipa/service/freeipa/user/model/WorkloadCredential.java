package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.google.common.collect.ImmutableList;

public class WorkloadCredential {

    private final String hashedPassword;

    private final ImmutableList<ActorKerberosKey> keys;

    private final Optional<Instant> expirationDate;

    public WorkloadCredential(String hashedPassword, List<ActorKerberosKey> keys, Optional<Instant> expirationDate) {
        this.hashedPassword = hashedPassword;
        this.keys = ImmutableList.copyOf(keys);
        this.expirationDate = expirationDate;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public List<ActorKerberosKey> getKeys() {
        return keys;
    }

    public Optional<Instant> getExpirationDate() {
        return expirationDate;
    }
}
