package com.sequenceiq.datalake.service.sdx;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;

@Service
public class InternalApiCallCalculator {

    public boolean isInternalApiCall(SecurityGroupV4Request securityGroup) {
        return !CollectionUtils.isEmpty(securityGroup.getSecurityGroupIds());
    }
}
