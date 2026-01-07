package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.updates.HostGroupV4RequestToHostGroupConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.ClusterOperationService;
import com.sequenceiq.cloudbreak.service.decorator.HostGroupDecorator;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.recipe.UpdateRecipeService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.UpdateNodeCountValidator;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class ClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommonService.class);

    @Inject
    private HostGroupDecorator hostGroupDecorator;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterOperationService clusterOperationService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private UpdateNodeCountValidator updateNodeCountValidator;

    @Inject
    private HostGroupV4RequestToHostGroupConverter hostGroupV4RequestToHostGroupConverter;

    @Inject
    private UpdateRecipeService updateRecipeService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public FlowIdentifier put(String crn, UpdateClusterV4Request updateJson) {
        StackDto stackDto = stackDtoService.getByCrn(crn);
        return put(stackDto, updateJson);
    }

    public FlowIdentifier put(StackDto stack, UpdateClusterV4Request updateJson) {
        Long stackId = stack.getId();
        MDCBuilder.buildMdcContext(stack);
        UserNamePasswordV4Request userNamePasswordJson = updateJson.getUserNamePassword();
        FlowIdentifier flowIdentifier;
        if (userNamePasswordJson != null) {
            flowIdentifier = clusterManagerUserNamePasswordChange(stack.getStack(), stack.getCluster(), userNamePasswordJson);
        } else if (updateJson.getStatus() != null) {
            LOGGER.debug("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            flowIdentifier = clusterOperationService.updateStatus(stackId, updateJson.getStatus());
        } else if (updateJson.getBlueprintName() != null && updateJson.getHostgroups() != null && stack.getStack().isCreateFailed()) {
            LOGGER.debug("Cluster rebuild request received. Stack id:  {}", stackId);
            try {
                flowIdentifier = recreateCluster(stack, updateJson);
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        } else if (updateJson.getHostGroupAdjustment() != null) {
            environmentService.checkEnvironmentStatus(stack.getStack(), EnvironmentStatus.upscalable());
            flowIdentifier = clusterHostgroupAdjustmentChange(stackId, updateJson, stack);
        } else {
            LOGGER.info("Invalid cluster update request received. Stack id: {}", stackId);
            throw new BadRequestException("Invalid update cluster request!");
        }
        return flowIdentifier;
    }

    public FlowIdentifier putVerticalScaling(String crn, StackVerticalScaleV4Request stackVerticalScaleV4Request) {
        StackDto stack = stackDtoService.getByCrn(crn);
        Long stackId = stack.getId();
        MDCBuilder.buildMdcContext(stack);
        FlowIdentifier flowIdentifier;
        if (stackVerticalScaleV4Request != null) {
            flowIdentifier = clusterOperationService.verticalScale(stackId, stackVerticalScaleV4Request);
        } else {
            LOGGER.info("Invalid cluster update request received. Stack id: {}", stackId);
            throw new BadRequestException("Invalid update cluster request!");
        }
        return flowIdentifier;
    }

    private FlowIdentifier clusterHostgroupAdjustmentChange(Long stackId, UpdateClusterV4Request updateJson, StackDto stack) {
        if (!stack.isAvailable() && !stack.getStack().hasNodeFailure()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' status. Cluster scale can only be made " +
                            "if the underlying stack status is 'AVAILABLE' or 'NODE_FAILURE'.", stackId, stack.getStatus()));
        }
        LOGGER.debug("Cluster host adjustment request received. Stack id: {} ", stackId);
        Blueprint blueprint = stack.getBlueprint();
        String hostGroupName = updateJson.getHostGroupAdjustment().getHostGroup();
        if (!hostGroupService.hasHostGroupInCluster(stack.getCluster().getId(), hostGroupName)) {
            throw new BadRequestException(String.format("Host group '%s' not found or not member of the cluster '%s'", hostGroupName, stack.getName()));
        }
        updateNodeCountValidator.validateScalabilityOfInstanceGroup(stack, updateJson.getHostGroupAdjustment());
        if (blueprintService.isClouderaManagerTemplate(blueprint)) {
            String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
            cmTemplateValidator.validateHostGroupScalingRequest(
                    accountId,
                    blueprint,
                    clusterComponentConfigProvider.getNormalizedCdhProductWithNormalizedVersion(stack.getCluster().getId()),
                    hostGroupName,
                    updateJson.getHostGroupAdjustment().getScalingAdjustment(),
                    instanceGroupService.findNotTerminatedByStackId(stack.getId()),
                    false);
        }
        return clusterOperationService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
    }

    private FlowIdentifier recreateCluster(StackDto stack, UpdateClusterV4Request updateCluster) throws TransactionExecutionException {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupV4Request json : updateCluster.getHostgroups()) {
            HostGroup hostGroup = hostGroupV4RequestToHostGroupConverter.convert(json);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, stack.getStack());
            hostGroups.add(hostGroup);
        }
        return clusterOperationService.recreate(stack, updateCluster.getBlueprintName(), hostGroups, updateCluster.getValidateBlueprint());
    }

    private FlowIdentifier clusterManagerUserNamePasswordChange(StackView stack, ClusterView cluster, UserNamePasswordV4Request userNamePasswordJson) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stack.getId(),
                    stack.getStatus()));
        }
        if (!userNamePasswordJson.getOldPassword().equals(cluster.getPassword())) {
            throw new BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password on Stack '%s' with status '%s'.", stack.getId(),
                    stack.getStatus()));
        }
        LOGGER.debug("Cluster username password update request received. Stack id: {}, name: {}, username: {}",
                stack.getId(), stack.getName(), userNamePasswordJson.getUserName());
        return clusterOperationService.updateUserNamePassword(stack.getId(), userNamePasswordJson);
    }

    public FlowIdentifier setMaintenanceMode(StackView stack, MaintenanceModeStatus maintenanceMode) {
        Long clusterId = stack.getClusterId();
        if (clusterId == null) {
            throw new BadRequestException(String.format("Cluster does not exist on stack with '%s' id.", stack.getId()));
        } else if (!stack.isAvailable() && !stack.isMaintenanceModeEnabled()) {
            throw new BadRequestException(String.format(
                    "Cluster '%s' is currently in '%s' state. Maintenance mode can be set to a cluster if it is 'AVAILABLE'.",
                    stack.getId(), stack.getStatus()));
        }

        FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
        switch (maintenanceMode) {
            case ENABLED:
                saveAndFireEventOnClusterStatusChange(stack, DetailedStackStatus.MAINTENANCE_MODE_ENABLED, ResourceEvent.MAINTENANCE_MODE_ENABLED);
                break;
            case DISABLED:
                saveAndFireEventOnClusterStatusChange(stack, DetailedStackStatus.AVAILABLE, ResourceEvent.MAINTENANCE_MODE_DISABLED);
                break;
            case VALIDATION_REQUESTED:
                if (!MAINTENANCE_MODE_ENABLED.equals(stack.getStatus())) {
                    throw new BadRequestException(String.format(
                            "Maintenance mode is not enabled for cluster '%s' (status:'%s'), it should be enabled before validation.",
                            clusterId, stack.getStatus()));
                }
                flowIdentifier = clusterOperationService.triggerMaintenanceModeValidation(stack.getId());
                break;
            default:
                // Nothing to do here
                break;

        }
        return flowIdentifier;
    }

    /**
     * Get cluster host details (ips + cluster name) - ini format
     *
     * @param stack     stack object that is used to fill the cluster details ini
     * @param loginUser ssh username that will be used as a default user in the inventory
     * @return Ini file content in string
     */
    public String getHostNamesAsIniString(Stack stack, String loginUser) {
        Cluster cluster = stack.getCluster();
        String clusterName = cluster.getName();
        String serverHost = cluster.getClusterManagerIp();

        List<InstanceMetaData> agentHostsSet = instanceMetaDataService.getAllInstanceMetadataByStackId(stack.getId())
                .stream().filter(i -> i.getInstanceStatus() != InstanceStatus.TERMINATED).collect(Collectors.toList());
        if (agentHostsSet.isEmpty()) {
            throw new NotFoundException(String.format("Not found any agent hosts (yet) for cluster '%s'", cluster.getId()));
        }
        String agentHosts = agentHostsSet.stream()
                .map(InstanceMetaData::getPublicIpWrapper)
                .collect(Collectors.joining("\n"));

        List<String> hostGroupHostsStrings = agentHostsSet.stream()
                .collect(Collectors.groupingBy(InstanceMetaData::getInstanceGroupName))
                .entrySet().stream()
                .map(s -> addSectionWithBody(
                        s.getKey(),
                        s.getValue().stream()
                                .map(InstanceMetaData::getPublicIpWrapper).collect(Collectors.joining("\n"))))
                .collect(Collectors.toList());

        return String.join(
                "\n",
                addSectionWithBody("cluster", "name=" + clusterName),
                addSectionWithBody("server", serverHost),
                String.join("\n", hostGroupHostsStrings),
                addSectionWithBody("agent", agentHosts),
                addSectionWithBody("all:vars",
                        String.join("\n", String.format("ansible_ssh_user=%s", loginUser),
                                "ansible_ssh_common_args='-o StrictHostKeyChecking=no'",
                                "ansible_become=yes"))
        );
    }

    private String addSectionWithBody(String section, String rawBody) {
        return String.format("[%s]%n%s%n", section, rawBody);
    }

    private void saveAndFireEventOnClusterStatusChange(StackView stack, DetailedStackStatus newDetailedStackStatus, ResourceEvent event) {
        Status actualStatus = stack.getStatus();
        if (!actualStatus.equals(newDetailedStackStatus.getStatus())) {
            clusterService.updateClusterStatusByStackId(stack.getId(), newDetailedStackStatus);
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), event.name(), event);
        }
    }

    public FlowIdentifier updateSalt(NameOrCrn nameOrCrn, String accountId, boolean skipHighstate) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        Status status = stack.getStatus();
        if (status.isStopState() || status.isTerminatedOrDeletionInProgress()) {
            String message = String.format("SaltStack update cannot be initiated as stack '%s' is currently in '%s' state.",
                    stack.getName(), status);
            LOGGER.info(message);
            throw new BadRequestException(message);
        } else {
            return clusterOperationService.updateSalt(stack.getId(), skipHighstate);
        }
    }

    public FlowIdentifier updatePillarConfiguration(NameOrCrn nameOrCrn, String accountId) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        if (stack.isStackInDeletionOrFailedPhase() || stack.isStopped() || stack.isStopInProgress()) {
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. Cannot start updates to the Pillar Configuration",
                    stack.getName(), stack.getStatus()));
        } else {
            return clusterOperationService.updatePillarConfiguration(stack.getId());
        }
    }

    public CertificatesRotationV4Response rotateAutoTlsCertificates(NameOrCrn nameOrCrn, String accountId,
            CertificatesRotationV4Request certificatesRotationV4Request) {
        StackView stack = stackDtoService.getStackViewByNameOrCrn(nameOrCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        validateOperationOnStack(stack, "Certificates rotation");
        FlowIdentifier flowIdentifier = clusterOperationService.rotateAutoTlsCertificates(stack.getId(), stack.getResourceCrn(), certificatesRotationV4Request);
        return new CertificatesRotationV4Response(flowIdentifier);
    }

    public UpdateRecipesV4Response refreshRecipes(NameOrCrn nameOrCrn, Long workspaceId, UpdateRecipesV4Request request) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        return updateRecipeService.refreshRecipesForCluster(workspaceId, stack, request.getHostGroupRecipes());

    }

    public AttachRecipeV4Response attachRecipe(NameOrCrn nameOrCrn, Long workspaceId, AttachRecipeV4Request request) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        return updateRecipeService.attachRecipeToCluster(workspaceId, stack, request.getRecipeName(), request.getHostGroupName());
    }

    public DetachRecipeV4Response detachRecipe(NameOrCrn nameOrCrn, Long workspaceId, DetachRecipeV4Request request) {
        Stack stack = stackService.getByNameOrCrnInWorkspace(nameOrCrn, workspaceId);
        MDCBuilder.buildMdcContext(stack);
        return updateRecipeService.detachRecipeFromCluster(workspaceId, stack, request.getRecipeName(), request.getHostGroupName());
    }

    private void validateOperationOnStack(StackView stack, String operationDescription) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format("Stack '%s' is currently in '%s' state. %s can only be made when the underlying stack is 'AVAILABLE'.",
                    stack.getName(), stack.getStatus(), operationDescription));
        } else if (stack.isDatalake() && instanceMetaDataService.anyInstanceStopped(stack.getId())) {
            throw new BadRequestException(String.format("Please start all stopped instances in Datalake. " +
                    "%s can only be made when all your nodes in running state.", operationDescription));
        }
    }

    public FlowIdentifier putDeleteVolumes(String crn, StackDeleteVolumesRequest deleteRequest) {
        StackDto stack = stackDtoService.getByCrn(crn);
        MDCBuilder.buildMdcContext(stack);
        if (deleteRequest != null) {
            return clusterOperationService.deleteVolumes(stack.getId(), deleteRequest);
        } else {
            LOGGER.info("Invalid cluster update request received. Stack id: {}",  stack.getId());
            throw new BadRequestException("Invalid update cluster request!");
        }
    }

    public FlowIdentifier putAddVolumes(String crn, StackAddVolumesRequest addVolumesRequest) {
        StackDto stack = stackDtoService.getByCrn(crn);
        MDCBuilder.buildMdcContext(stack);
        if (addVolumesRequest != null) {
            return clusterOperationService.addVolumes(stack.getId(), addVolumesRequest);
        } else {
            LOGGER.info("Invalid cluster update request received. Stack id: {}",  stack.getId());
            throw new BadRequestException("Invalid update cluster request!");
        }
    }
}
