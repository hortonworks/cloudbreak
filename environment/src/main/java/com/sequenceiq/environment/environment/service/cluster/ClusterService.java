package com.sequenceiq.environment.environment.service.cluster;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Service
public class ClusterService {

    private final StackV4Endpoint stackV4Endpoint;

    public ClusterService(StackV4Endpoint stackV4Endpoint) {
        this.stackV4Endpoint = stackV4Endpoint;
    }

    public List<String> getClustersNamesByEncrytionProfile(String encryptionProfileName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return stackV4Endpoint.getClustersNamesByEncrytionProfile(0L, encryptionProfileName, accountId);
    }
}
