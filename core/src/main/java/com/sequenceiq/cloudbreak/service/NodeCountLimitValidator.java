package com.sequenceiq.cloudbreak.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class NodeCountLimitValidator {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private LimitConfiguration nodeCountLimitConfiguration;

    public void validateScale(Long stackId, Integer scalingAdjustment, String accountId) {
        if (scalingAdjustment > 0) {
            Integer currentNodeCount = instanceMetaDataService.countByStackId(stackId).getInstanceCount();
            validateNodeCount(currentNodeCount + scalingAdjustment, accountId);
        }
    }

    public void validateProvision(StackV4Request stackRequest) {
        validateNodeCount(stackRequest.getInstanceGroups().stream().mapToInt(InstanceGroupV4Base::getNodeCount).sum(),
                Crn.safeFromString(stackRequest.getEnvironmentCrn()).getAccountId());
    }

    private void validateNodeCount(Integer targetNodeCount, String accountId) {
        Integer nodeCountLimit = nodeCountLimitConfiguration.getNodeCountLimit(Optional.ofNullable(accountId));
        if (targetNodeCount > nodeCountLimit) {
            throw new BadRequestException(String.format("The maximum count of nodes for this cluster cannot be higher than %s", nodeCountLimit));
        }
    }
}
