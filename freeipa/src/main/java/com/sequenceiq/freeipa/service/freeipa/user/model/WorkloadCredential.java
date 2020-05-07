package com.sequenceiq.freeipa.service.freeipa.user.model;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.SshPublicKey;
import com.google.common.collect.ImmutableList;

public class WorkloadCredential {

    private final String hashedPassword;

    private final ImmutableList<ActorKerberosKey> keys;

    private final Optional<Instant> expirationDate;

    private final ImmutableList<SshPublicKey> sshPublicKeys;

    public WorkloadCredential(String hashedPassword, Collection<ActorKerberosKey> keys, Optional<Instant> expirationDate,
            Collection<SshPublicKey> sshPublicKeys) {
        this.hashedPassword = hashedPassword;
        this.keys = ImmutableList.copyOf(keys);
        this.expirationDate = expirationDate;
        this.sshPublicKeys = ImmutableList.copyOf(sshPublicKeys);
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

    public ImmutableList<SshPublicKey> getSshPublicKeys() {
        return sshPublicKeys;
    }
}
