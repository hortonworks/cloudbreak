package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService.UNDEFINED_DEPENDENCY;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cluster.api.ClusterHealthService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DependentRolesHealthCheckService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDownscaleValidatorService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class UpdateNodeCountValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateNodeCountValidator.class);

    private static final String GOOD_HEALTH = "GOOD";

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private DependentRolesHealthCheckService dependentRolesHealthCheckService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    public void validataHostMetadataStatuses(StackDto stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
            List<InstanceMetadataView> instanceMetaDataAsList = stack.getNotTerminatedInstanceMetaData();
            List<InstanceMetadataView> unhealthyInstanceMetadataList = instanceMetaDataAsList.stream()
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

    public void validateHostGroupIsPresent(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, StackDto stack) {
        Optional<InstanceGroupView> hostGroup = stack.getInstanceGroupViews().stream()
                .filter(input -> input.getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup())).findFirst();
        if (hostGroup.isEmpty()) {
            throw new BadRequestException(format("Group '%s' not found or not part of Data Hub '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
    }

    public void validateCMStatus(StackDto stack) {
        List<InstanceMetadataView> instanceMetaDataAsList = stack.getAllAvailableInstances();
        List<InstanceMetadataView> unhealthyCM = instanceMetaDataAsList.stream()
                .filter(instanceMetaData -> instanceMetaData.getInstanceMetadataType().equals(InstanceMetadataType.GATEWAY_PRIMARY)
                        && !InstanceStatus.SERVICES_HEALTHY.equals(instanceMetaData.getInstanceStatus()))
                .collect(Collectors.toList());
        if (!unhealthyCM.isEmpty()) {
            String notHealthyInstances = unhealthyCM.stream()
                    .map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() + ": " + instanceMetaData.getInstanceStatus())
                    .collect(Collectors.joining(","));
            throw new BadRequestException(
                    format("Upscale is not allowed because the CM host is not healthy: %s.", notHealthyInstances));
        }
    }

    public void validateStackStatus(StackView stack, boolean upscale) {
        if (upscale &&
                !(stack.isAvailable() || stack.hasNodeFailure() && targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(stack.getResourceCrn()))) {
            throwBadRequest(stack);
        } else if (!upscale && !stack.isAvailable()) {
            throwBadRequest(stack);
        }
    }

    public void validateClusterStatus(StackView stack, boolean upscale) {
        Long clusterId = stack.getClusterId();
        if (upscale && clusterId != null && !(stack.isAvailable()
                || stack.hasNodeFailure() && targetedUpscaleSupportService.targetedUpscaleEntitlementsEnabled(stack.getResourceCrn()))) {
            throwBadRequest(stack);
        } else if (!upscale && clusterId != null && !stack.isAvailable()) {
            throwBadRequest(stack);
        }
    }

    private void throwBadRequest(StackView stack) {
        throw new BadRequestException(format("Data Hub '%s' is currently in '%s' state. Node count can only be updated if it's running.",
                stack.getName(), stack.getStatus()));
    }

    public void validateStackStatusForStopStartHostGroup(StackDto stack, String instanceGroup, Integer scalingAdjustment) {

        if (stack.getStack().isModificationInProgress()) {
            if (scalingAdjustment > 0) {
                throw new BadRequestException(format("Data Hub '%s' has '%s' state. Upscaling is not allowed.",
                        stack.getStack().getName(), stack.getStack().getStatus()));
            } else if (scalingAdjustment < 0) {
                throw new BadRequestException(format("Data Hub '%s' has '%s' state. Downscaling is not allowed.",
                        stack.getStack().getName(), stack.getStack().getStatus()));
            }
        }

        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
        Set<String> dependentComponents = dependentRolesHealthCheckService.getDependentComponentsForHostGroup(processor,
                instanceGroup);

        if (dependentComponents.contains(UNDEFINED_DEPENDENCY)) {
            if (!(stack.getStack().isAvailable() || stack.getStack().isAvailableWithStoppedInstances())) {
                throw new BadRequestException(format("Data Hub '%s' has '%s' state. Node group start operation is not allowed for this state.",
                        stack.getStack().getName(), stack.getStack().getStatus()));
            }
        } else {
            List<String> unhealthyHostGroupNames = dependentRolesHealthCheckService.getUnhealthyDependentHostGroups(stack, processor, dependentComponents);
            if (!unhealthyHostGroupNames.isEmpty()) {
                LOGGER.info("Unhealthy Hostgroups: {}", unhealthyHostGroupNames);
                Set<String> dependentComponentsHealthCheckNames = dependentRolesHealthCheckService.getDependentComponentsHeathChecksForHostGroup(processor,
                        instanceGroup);
                Set<String> unhealthyHealthChecks = getUnhealthyHealthChecks(dependentComponentsHealthCheckNames, stack);
                if (!unhealthyHealthChecks.isEmpty()) {
                    throw new BadRequestException(format("%s is not allowed for HostGroup: '%s' as Data hub '%s' has " +
                                    "health checks which are not healthy: [%s] for instances in hostGroup(s): [%s]",
                            (scalingAdjustment > 0) ? "Upscaling" : "Downscaling", instanceGroup, stack.getStack().getName(), unhealthyHealthChecks,
                            unhealthyHostGroupNames));
                }
            }
        }
    }

    private Set<String> getUnhealthyHealthChecks(Set<String> healthCheckNames, StackDto stack) {
        ClusterHealthService clusterHealthService = clusterApiConnectors.getConnector(stack).clusterHealthService();
        Map<String, String> hostServicesHealth = clusterHealthService.readServicesHealth(stack.getStack().getName());
        Set<String> unhealthyHealthChecks = healthCheckNames.stream()
                .filter(healthCheck -> !GOOD_HEALTH.equalsIgnoreCase(hostServicesHealth.getOrDefault(healthCheck, "BAD")))
                .collect(Collectors.toSet());
        LOGGER.info("Health checks having bad or concerning health are: {}", unhealthyHealthChecks);
        return unhealthyHealthChecks;
    }

    public void validateServiceRoles(StackDto stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        validateServiceRoles(stack, instanceGroupAdjustmentJson.getInstanceGroup(), instanceGroupAdjustmentJson.getScalingAdjustment(), false);
    }

    public void validateServiceRoles(StackDto stack, Map<String, Integer> instanceGroupAdjustments, boolean forced) {
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        cmTemplateValidator.validateHostGroupScalingRequest(
                accountId,
                stack.getBlueprint(),
                instanceGroupAdjustments,
                clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(stack.getCluster().getId()),
                instanceGroupService.findNotTerminatedByStackId(stack.getId()),
                forced);
    }

    public void validateServiceRoles(StackDto stack, String instanceGroup, int scalingAdjustment, boolean forced) {
        if (hostGroupService.hasHostGroupInCluster(stack.getCluster().getId(), instanceGroup)) {
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            cmTemplateValidator.validateHostGroupScalingRequest(
                    accountId,
                    stack.getBlueprint(),
                    clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(stack.getCluster().getId()),
                    instanceGroup,
                    scalingAdjustment,
                    instanceGroupService.findNotTerminatedByStackId(stack.getId()),
                    forced);
        }
    }

    public void validateInstanceGroup(StackDto stack, String instanceGroupName) {
        InstanceGroupDto instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(format("Data Hub '%s' does not have a group named '%s'.", stack.getName(), instanceGroupName));
        }
    }

    public void validateScalabilityOfInstanceGroup(StackDto stack, HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request) {
        validateScalabilityOfInstanceGroup(stack,
                hostGroupAdjustmentV4Request.getHostGroup(),
                hostGroupAdjustmentV4Request.getScalingAdjustment());
    }

    public void validateScalabilityOfInstanceGroup(StackDto stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        validateScalabilityOfInstanceGroup(stack,
                instanceGroupAdjustmentJson.getInstanceGroup(),
                instanceGroupAdjustmentJson.getScalingAdjustment());
    }

    public void validateScalabilityOfInstanceGroup(StackDto stack, String instanceGroupName, int scalingAdjustment) {
        InstanceGroupDto instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        validateGroupAdjustment(
                stack.getStack(),
                scalingAdjustment,
                instanceGroup);
    }

    public void validateInstanceGroupForStopStart(StackDto stack, String instanceGroupName, int scalingAdjustment) {
        Set<String> computeGroups = getComputeHostGroup(stack);
        if (!computeGroups.contains(instanceGroupName)) {
            if (upscaleEvent(scalingAdjustment)) {
                throw new BadRequestException(format("Start instances operation is not allowed for %s host group.", instanceGroupName));
            } else if (downScaleEvent(scalingAdjustment)) {
                throw new BadRequestException(format("Stop instances operation is not allowed for %s host group.", instanceGroupName));
            } else {
                throw new BadRequestException(format("Zero Scaling adjustment detected for %s host group.", instanceGroupName));
            }
        }
    }

    private void validateGroupAdjustment(StackView stack, Integer scalingAdjustment, InstanceGroupDto instanceGroupDto) {
        InstanceGroupView instanceGroup = instanceGroupDto.getInstanceGroup();
        if (upscaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroupDto, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group because the " +
                                "the current node count is %s node the node count after the upscale action will be %s node and the minimal " +
                                "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroupDto.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroupDto, scalingAdjustment),
                        instanceGroup.getGroupName(),
                        instanceGroup.getMinimumNodeCount()));
            }
            if (!instanceGroup.getScalabilityOption().upscalable()) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        } else if (downScaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroupDto, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub %s group because the " +
                                "the current node count is %s node the node count after the downscale action will be %s node and the minimal " +
                                "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroupDto.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroupDto, scalingAdjustment),
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

    private int getNodeCountAfterScaling(InstanceGroupDto instanceGroup, Integer scalingAdjustment) {
        return instanceGroup.getNodeCount() + scalingAdjustment.intValue();
    }

    private Set<String> getComputeHostGroup(StackDto stack) {
        CmTemplateProcessor templateProcessor = cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
        Versioned version = () -> templateProcessor.getVersion().get();
        return templateProcessor.getComputeHostGroups(version);
    }

    private boolean nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(InstanceGroupDto instanceGroupDto, Integer scalingAdjustment) {
        InstanceGroupView instanceGroup = instanceGroupDto.getInstanceGroup();
        int minimumNodeCount = instanceGroup.getMinimumNodeCount();
        return getNodeCountAfterScaling(instanceGroupDto, scalingAdjustment) < minimumNodeCount;
    }

    private boolean downScaleEvent(Integer scalingAdjustment) {
        return 0 > scalingAdjustment;
    }

    private boolean upscaleEvent(Integer scalingAdjustment) {
        return 0 < scalingAdjustment;
    }

    public void validateScalingAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, StackDto stack) {
        validateScalingAdjustment(instanceGroupAdjustmentJson.getInstanceGroup(),
                instanceGroupAdjustmentJson.getScalingAdjustment(),
                stack);
    }

    public void validateScalingAdjustment(String instanceGroupName, Integer scalingAdjustment, StackDto stack) {
        InstanceGroupDto instanceGroupDto = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        InstanceGroupView instanceGroup = instanceGroupDto.getInstanceGroup();
        if (upscaleEvent(scalingAdjustment)) {
            if (!instanceGroup.getScalabilityOption().upscalable()) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        } else if (downScaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroupDto, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub %s group because the " +
                                "the current node count is %s node the node count after the downscale action will be %s and the minimal " +
                                "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroupDto.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroupDto, scalingAdjustment),
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

    public void validateInstanceStatuses(StackDtoDelegate stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
            List<InstanceMetadataView> instanceMetaDataList =
                    stack.getNotTerminatedInstanceMetaData().stream().filter(im -> !im.isTerminated() && !im.isRunning() && !im.isCreated())
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

    public InstanceMetaData validateInstanceForDownscale(String instanceId, StackView stack) {
        InstanceMetaData metaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId)
                .orElseThrow(() -> new NotFoundException(format("Metadata for instance %s has not found.", instanceId)));
        downscaleValidatorService.checkInstanceIsTheClusterManagerServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack);
        return metaData;
    }

    public InstanceMetaData validateInstanceForStop(String instanceId, StackView stack) {
        InstanceMetaData metaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId).orElse(null);
        if (metaData != null) {
            downscaleValidatorService.checkInstanceIsTheClusterManagerServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
            downscaleValidatorService.checkClusterInValidStatus(stack);
        }
        return metaData;
    }
}
