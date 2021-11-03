package com.sequenceiq.cloudbreak.service.stack.flow;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDownscaleValidatorService;

@Component
public class UpdateNodeCountValidator {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    @Inject
    private InstanceGroupService instanceGroupService;

    public void validataHostMetadataStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
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
            throw new BadRequestException(format("Group '%s' not found or not part of Data Hub '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
    }

    public void validateStackStatus(Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(format("Group '%s' is currently in '%s' state. Node count can only be updated if it's running.",
                    stack.getName(), stack.getStatus()));
        }
    }

    public void validateServiceRoles(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        validateServiceRoles(stack, instanceGroupAdjustmentJson.getInstanceGroup(), instanceGroupAdjustmentJson.getScalingAdjustment());
    }

    public void validateServiceRoles(Stack stack, String instanceGroup, int scalingAdjustment) {
        Optional<HostGroup> hostGroup = hostGroupService.findHostGroupsInCluster(stack.getCluster().getId())
                .stream()
                .filter(e -> e.getName().equals(instanceGroup))
                .findFirst();
        if (hostGroup.isPresent()) {
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            cmTemplateValidator.validateHostGroupScalingRequest(
                    accountId,
                    stack.getCluster().getBlueprint(),
                    hostGroup.get(),
                    scalingAdjustment,
                    instanceGroupService.findNotTerminatedByStackId(stack.getId()));
        }
    }

    public void validateClusterStatus(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null && !cluster.isAvailable()) {
            throw new BadRequestException(format("Data Hub '%s' is currently in '%s' state. Node count can only be updated if it's not available.",
                    cluster.getName(), cluster.getStatus()));
        }
    }

    public void validateInstanceGroup(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(format("Data Hub '%s' does not have a group named '%s'.", stack.getName(), instanceGroupName));
        }
    }

    public void validateScalabilityOfInstanceGroup(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request) {
        validateScalabilityOfInstanceGroup(stack,
                hostGroupAdjustmentV4Request.getHostGroup(),
                hostGroupAdjustmentV4Request.getScalingAdjustment());
    }

    public void validateScalabilityOfInstanceGroup(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        validateScalabilityOfInstanceGroup(stack,
                instanceGroupAdjustmentJson.getInstanceGroup(),
                instanceGroupAdjustmentJson.getScalingAdjustment());
    }

    public void validateScalabilityOfInstanceGroup(Stack stack, String instanceGroupName, int scalingAdjustment) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        validateGroupAdjustment(
                stack,
                scalingAdjustment,
                instanceGroup);
    }

    private void validateGroupAdjustment(Stack stack, Integer scalingAdjustment, InstanceGroup instanceGroup) {
        if (upscaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroup, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group because the " +
                        "the current node count is %s node the node count after the upscale action will be %s node and the minimal " +
                        "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroup.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroup, scalingAdjustment),
                        instanceGroup.getGroupName(),
                        instanceGroup.getMinimumNodeCount()));
            }
            if (!instanceGroup.getScalabilityOption().upscalable()) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        } else if (downScaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroup, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub %s group because the " +
                        "the current node count is %s node the node count after the downscale action will be %s node and the minimal " +
                        "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroup.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroup, scalingAdjustment),
                        instanceGroup.getGroupName(),
                        instanceGroup.getMinimumNodeCount()));
            }
            if (!instanceGroup.getScalabilityOption().downscalable()) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub's %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        }
    }

    private int getNodeCountAfterScaling(InstanceGroup instanceGroup, Integer scalingAdjustment) {
        return instanceGroup.getNodeCount() + scalingAdjustment.intValue();
    }

    private boolean nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(InstanceGroup instanceGroup,
        Integer scalingAdjustment) {
        int minimumNodeCount = instanceGroup.getMinimumNodeCount();
        return getNodeCountAfterScaling(instanceGroup, scalingAdjustment) < minimumNodeCount;
    }

    private boolean downScaleEvent(Integer scalingAdjustment) {
        return 0 > scalingAdjustment;
    }

    private boolean upscaleEvent(Integer scalingAdjustment) {
        return 0 < scalingAdjustment;
    }

    public void validateScalingAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack) {
        validateScalingAdjustment(instanceGroupAdjustmentJson.getInstanceGroup(),
                instanceGroupAdjustmentJson.getScalingAdjustment(),
                stack);
    }

    public void validateScalingAdjustment(String instanceGroupName, Integer scalingAdjustment, Stack stack) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (upscaleEvent(scalingAdjustment)) {
            if (!instanceGroup.getScalabilityOption().upscalable()) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        } else if (downScaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroup, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub %s group because the " +
                                "the current node count is %s node the node count after the downscale action will be %s and the minimal " +
                                "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroup.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroup, scalingAdjustment),
                        instanceGroup.getGroupName(),
                        instanceGroup.getMinimumNodeCount()));
            }
            if (!instanceGroup.getScalabilityOption().downscalable()) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub's %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        }
    }

    public void validateInstanceStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
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

    public InstanceMetaData validateInstanceForDownscale(String instanceId, Stack stack) {
        InstanceMetaData metaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId)
                .orElseThrow(() -> new NotFoundException(format("Metadata for instance %s has not found.", instanceId)));
        downscaleValidatorService.checkInstanceIsTheClusterManagerServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack.getCluster());
        return metaData;
    }
}
