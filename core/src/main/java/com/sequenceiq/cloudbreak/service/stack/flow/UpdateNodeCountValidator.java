package com.sequenceiq.cloudbreak.service.stack.flow;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDownscaleValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Component
public class UpdateNodeCountValidator {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private CommonPermissionCheckingUtils permissionCheckingUtils;

    public void validataHostMetadataStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            List<InstanceMetaData> instanceMetaDataAsList = stack.getInstanceMetaDataAsList();
            List<InstanceMetaData> unhealthyInstanceMetadataList = instanceMetaDataAsList.stream()
                    .filter(instanceMetaData -> InstanceStatus.SERVICES_UNHEALTHY.equals(instanceMetaData.getInstanceStatus()))
                    .collect(Collectors.toList());
            if (!unhealthyInstanceMetadataList.isEmpty()) {
                String notHealthyInstances = unhealthyInstanceMetadataList.stream()
                        .map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() + ": " + instanceMetaData.getInstanceStatus())
                        .collect(Collectors.joining(","));
                throw new BadRequestException(
                        format("Upscale is not allowed because the following hosts are not healthy: %s. Please remove them first!", notHealthyInstances));
            }
        }
    }

    public void validateHostGroupAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack, Integer adjustment) {
        Optional<HostGroup> hostGroup = stack.getCluster().getHostGroups().stream()
                .filter(input -> input.getInstanceGroup().getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup())).findFirst();
        if (!hostGroup.isPresent()) {
            throw new BadRequestException(format("Instancegroup '%s' not found or not part of stack '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
    }

    public void validateStackStatus(Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(format("Stack '%s' is currently in '%s' state. Node count can only be updated if it's running.",
                    stack.getName(), stack.getStatus()));
        }
    }

    public void validateClusterStatus(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null && !cluster.isAvailable()) {
            throw new BadRequestException(format("Cluster '%s' is currently in '%s' state. Node count can only be updated if it's not available.",
                    cluster.getName(), cluster.getStatus()));
        }
    }

    public void validateInstanceGroup(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(format("Stack '%s' does not have an instanceGroup named '%s'.", stack.getName(), instanceGroupName));
        }
    }

    public void validateScalingAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack) {
        if (0 == instanceGroupAdjustmentJson.getScalingAdjustment()) {
            throw new BadRequestException(format("Requested scaling adjustment on stack '%s' is 0. Nothing to do.", stack.getName()));
        }
        if (0 > instanceGroupAdjustmentJson.getScalingAdjustment()) {
            InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupAdjustmentJson.getInstanceGroup());
            if (-1 * instanceGroupAdjustmentJson.getScalingAdjustment() > instanceGroup.getNodeCount()) {
                throw new BadRequestException(format("There are %s instances in instance group '%s'. Cannot remove %s instances.",
                        instanceGroup.getNodeCount(), instanceGroup.getGroupName(),
                        -1 * instanceGroupAdjustmentJson.getScalingAdjustment()));
            }
            int removableHosts = instanceMetaDataService.findRemovableInstances(stack.getId(), instanceGroupAdjustmentJson.getInstanceGroup()).size();
            if (removableHosts < -1 * instanceGroupAdjustmentJson.getScalingAdjustment()) {
                throw new BadRequestException(
                        format("There are %s unregistered instances in instance group '%s' but %s were requested. Decommission nodes from the cluster!",
                                removableHosts, instanceGroup.getGroupName(), instanceGroupAdjustmentJson.getScalingAdjustment() * -1));
            }
        }
    }

    public void validateInstanceStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (instanceGroupAdjustmentJson.getScalingAdjustment() > 0) {
            List<InstanceMetaData> instanceMetaDataList =
                    stack.getInstanceMetaDataAsList().stream().filter(im -> !im.isTerminated() && !im.isRunning() && !im.isCreated())
                            .collect(Collectors.toList());
            if (!instanceMetaDataList.isEmpty()) {
                String ims = instanceMetaDataList.stream()
                        .map(im -> im.getInstanceId() != null ? im.getInstanceId() : im.getPrivateId() + ": " + im.getInstanceStatus())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(
                        format("Upscale is not allowed because the following instances are not in running state: %s. Please remove them first!", ims));
            }
        }
    }

    public InstanceMetaData validateInstanceForDownscale(String instanceId, Stack stack, Long workspaceId, User user) {
        permissionCheckingUtils.checkPermissionForUser(AuthorizationResourceType.DATAHUB, AuthorizationResourceAction.WRITE, user.getUserCrn());
        InstanceMetaData metaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId)
                .orElseThrow(() -> new NotFoundException(format("Metadata for instance %s has not found.", instanceId)));
        downscaleValidatorService.checkInstanceIsTheClusterManagerServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack.getCluster());
        return metaData;
    }
}
