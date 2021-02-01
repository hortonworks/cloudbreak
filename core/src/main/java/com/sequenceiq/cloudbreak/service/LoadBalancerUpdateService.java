package com.sequenceiq.cloudbreak.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class LoadBalancerUpdateService {

    public FlowIdentifier updateLoadBalancers(NameOrCrn nameOrCrn, Long workspaceId) {
        return null;
    }
}
