package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Component
public class UpdateHostsValidator {

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public boolean validateRequest(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        HostGroup hostGroup = getHostGroup(stack, hostGroupAdjustment);
        int scalingAdjustment = hostGroupAdjustment.getScalingAdjustment();
        boolean downScale = scalingAdjustment < 0;
        if (scalingAdjustment == 0) {
            throw new BadRequestException("No scaling adjustments specified. Nothing to do.");
        }
        if (!downScale && hostGroup.getInstanceGroup() != null) {
            validateUnusedHosts(hostGroup.getInstanceGroup(), scalingAdjustment);
        } else {
            validateRegisteredHosts(stack, hostGroupAdjustment);
            if (hostGroupAdjustment.getWithStackUpdate() && hostGroupAdjustment.getScalingAdjustment() > 0) {
                throw new BadRequestException("ScalingAdjustment has to be decommission if you define withStackUpdate = 'true'.");
            }
        }
        return downScale;
    }

    private void validateUnusedHosts(InstanceGroup instanceGroup, int scalingAdjustment) {
        Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetaDataService.findUnusedHostsInInstanceGroup(instanceGroup.getId());
        if (unusedHostsInInstanceGroup.size() < scalingAdjustment) {
            throw new BadRequestException(String.format(
                    "There are %s unregistered instances in instance group '%s'. %s more instances needed to complete this request.",
                    unusedHostsInInstanceGroup.size(), instanceGroup.getGroupName(), scalingAdjustment - unusedHostsInInstanceGroup.size()));
        }
    }

    private void validateRegisteredHosts(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        String hostGroupName = hostGroupAdjustment.getHostGroup();
        hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName).ifPresentOrElse(hostGroup -> {
            if (hostGroup.getInstanceGroup() == null) {
                throw new BadRequestException(String.format("Can't find instancegroup for hostgroup: %s", hostGroupName));
            } else {
                InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
                int hostsCount = instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet().size();
                int adjustment = Math.abs(hostGroupAdjustment.getScalingAdjustment());
                Boolean validateNodeCount = hostGroupAdjustment.getValidateNodeCount();
                if (validateNodeCount == null || validateNodeCount) {
                    if (hostsCount <= adjustment) {
                        String errorMessage = String.format("[hostGroup: '%s', current hosts: %s, decommissions requested: %s]",
                                hostGroupName, hostsCount, adjustment);
                        throw new BadRequestException(String.format("The host group must contain at least 1 host after the decommission: %s", errorMessage));
                    }
                } else if (hostsCount - adjustment < 0) {
                    throw new BadRequestException(String.format("There are not enough hosts in host group: %s to remove", hostGroupName));
                }
            }
        }, () -> {
            throw new BadRequestException(String.format("Can't find hostgroup: %s", hostGroupName));
        });
    }

    private HostGroup getHostGroup(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustment) {
        Optional<HostGroup> hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupAdjustment.getHostGroup());
        if (hostGroup.isEmpty()) {
            throw new BadRequestException(String.format(
                    "Invalid host group: cluster '%s' does not contain a host group named '%s'.",
                    stack.getCluster().getName(), hostGroupAdjustment.getHostGroup()));
        }
        return hostGroup.get();
    }
}
