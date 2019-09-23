package com.sequenceiq.freeipa.util;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;

import java.util.List;

public final class WorkloadCredentialsUtils {

    private WorkloadCredentialsUtils() {

    }

    public static String getEncodedKrbPrincipalKey(List<ActorKerberosKey> keys) throws Exception {
        if (keys == null || keys.size() == 0) {
            throw new IllegalArgumentException("Invalid kerberos keys provided");
        }

        // getEncodedKrbPrincilaKey for both the keys one by one and return
        // TODO: process both keys
        ActorKerberosKey key = keys.get(0);
        String salt = key.getSaltValue();

        return ASNEncoder.getASNEncodedKrbPrincipalKey(salt);
    }
}
