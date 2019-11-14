package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.MaintenanceModeStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.HostGroupDecorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Service
public class ClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommonService.class);

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private HostGroupDecorator hostGroupDecorator;

    @Inject
    private ClusterService clusterService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private BlueprintService blueprintService;

    public void put(String crn, UpdateClusterV4Request updateJson, User user, Workspace workspace) {
        Stack stack = stackService.getByCrn(crn);
        Long stackId = stack.getId();
        MDCBuilder.buildMdcContext(stack);
        UserNamePasswordV4Request userNamePasswordJson = updateJson.getUserNamePassword();
        if (userNamePasswordJson != null) {
            ambariUserNamePasswordChange(stackId, stack, userNamePasswordJson);
        } else if (updateJson.getStatus() != null) {
            LOGGER.debug("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            clusterService.updateStatus(stackId, updateJson.getStatus());
        } else if (updateJson.getBlueprintName() != null && updateJson.getHostgroups() != null && stack.getCluster().isCreateFailed()) {
            LOGGER.debug("Cluster rebuild request received. Stack id:  {}", stackId);
            try {
                recreateCluster(stack, updateJson, user, workspace);
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
        } else if (updateJson.getHostGroupAdjustment() != null) {
            clusterHostgroupAdjustmentChange(stackId, updateJson, stack);
        } else if (Objects.nonNull(updateJson.getStackRepository())) {
            updateStackDetails(updateJson, stack);
        } else {
            LOGGER.info("Invalid cluster update request received. Stack id: {}", stackId);
            throw new BadRequestException("Invalid update cluster request!");
        }
    }

    private void updateStackDetails(UpdateClusterV4Request updateJson, Stack stack) {
        Cluster cluster = stack.getCluster();
        if (!cluster.isMaintenanceModeEnabled()) {
            throw new BadRequestException(String.format("Repo update is only permitted in maintenance mode, current status is: '%s'",
                    stack.getCluster().getStatus()));
        }

        StackRepositoryV4Request stackRepository = updateJson.getStackRepository();
        Long clusterId = cluster.getId();
        if ("AMBARI".equals(stackRepository.getStack())) {
            clusterService.updateAmbariRepoDetails(clusterId, stackRepository);
        } else if ("HDP".equals(stackRepository.getStack())) {
            clusterService.updateHdpRepoDetails(clusterId, stackRepository);
        } else if ("HDF".equals(stackRepository.getStack())) {
            clusterService.updateHdfRepoDetails(clusterId, stackRepository);
        }
    }

    private void clusterHostgroupAdjustmentChange(Long stackId, UpdateClusterV4Request updateJson, Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.debug("Cluster host adjustment request received. Stack id: {} ", stackId);
        Blueprint blueprint = stack.getCluster().getBlueprint();
        Optional<HostGroup> hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(),
                updateJson.getHostGroupAdjustment().getHostGroup());
        if (hostGroup.isEmpty()) {
            throw new BadRequestException(String.format("Host group '%s' not found or not member of the cluster '%s'",
                    updateJson.getHostGroupAdjustment().getHostGroup(), stack.getName()));
        }
        if (blueprintService.isAmbariBlueprint(blueprint)) {
            ambariBlueprintValidator.validateHostGroupScalingRequest(blueprint, hostGroup.get(),
                    updateJson.getHostGroupAdjustment().getScalingAdjustment());
        } else {
            cmTemplateValidator.validateHostGroupScalingRequest(blueprint, hostGroup.get(),
                    updateJson.getHostGroupAdjustment().getScalingAdjustment());
        }
        clusterService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
    }

    private void recreateCluster(Stack stack, UpdateClusterV4Request updateCluster, User user, Workspace workspace) throws TransactionExecutionException {
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupV4Request json : updateCluster.getHostgroups()) {
            HostGroup hostGroup = converterUtil.convert(json, HostGroup.class);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, stack, false);
            hostGroups.add(hostGroup);
        }
        StackRepositoryV4Request stackDetails = updateCluster.getStackRepository();
        StackRepoDetails stackRepoDetails = null;
        if (stackDetails != null) {
            stackRepoDetails = converterUtil.convert(stackDetails, StackRepoDetails.class);
        }
        clusterService.recreate(stack, updateCluster.getBlueprintName(), hostGroups, updateCluster.getValidateBlueprint(), stackRepoDetails);
    }

    private void ambariUserNamePasswordChange(Long stackId, Stack stack, UserNamePasswordV4Request userNamePasswordJson) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        if (!userNamePasswordJson.getOldPassword().equals(stack.getCluster().getPassword())) {
            throw new BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password on Stack '%s' with status '%s'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.debug("Cluster username password update request received. Stack id:  {}, username: {}",
                stackId, userNamePasswordJson.getUserName());
        clusterService.updateUserNamePassword(stackId, userNamePasswordJson);
    }

    public void setMaintenanceMode(Stack stack, MaintenanceModeStatus maintenanceMode) {
        Cluster cluster = stack.getCluster();
        if (cluster == null) {
            throw new BadRequestException(String.format("Cluster does not exist on stack with '%s' id.", stack.getId()));
        } else if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. Maintenance mode can be set to a cluster if the underlying stack is 'AVAILABLE'.",
                    stack.getId(), stack.getStatus()));
        } else if (!cluster.isAvailable() && !cluster.isMaintenanceModeEnabled()) {
            throw new BadRequestException(String.format(
                    "Cluster '%s' is currently in '%s' state. Maintenance mode can be set to a cluster if it is 'AVAILABLE'.",
                    cluster.getId(), cluster.getStatus()));
        }

        switch (maintenanceMode) {
            case ENABLED:
                saveAndFireEventOnClusterStatusChange(cluster, stack.getId(), MAINTENANCE_MODE_ENABLED, ResourceEvent.MAINTENANCE_MODE_ENABLED);
                break;
            case DISABLED:
                saveAndFireEventOnClusterStatusChange(cluster, stack.getId(), AVAILABLE, ResourceEvent.MAINTENANCE_MODE_DISABLED);
                break;
            case VALIDATION_REQUESTED:
                if (!MAINTENANCE_MODE_ENABLED.equals(cluster.getStatus())) {
                    throw new BadRequestException(String.format(
                            "Maintenance mode is not enabled for cluster '%s' (status:'%s'), it should be enabled before validation.",
                            cluster.getId(),
                            cluster.getStatus()));
                }
                clusterService.triggerMaintenanceModeValidation(stack);
                clusterService.save(cluster);
                break;
            default:
                // Nothing to do here
                break;

        }
    }

    /**
     * Get cluster host details (ips + cluster name) - ini format
     * @param stack stack object that is used to fill the cluster details ini
     * @param loginUser ssh username that will be used as a default user in the inventory
     * @return Ini file content in string
     */
    public String getHostNamesAsIniString(Stack stack, String loginUser) {
        Cluster cluster = stack.getCluster();
        String clusterName = cluster.getName();
        String serverHost = cluster.getClusterManagerIp();

        Set<InstanceMetaData> agentHostsSet = stack.getRunningInstanceMetaDataSet();
        if (agentHostsSet.isEmpty()) {
            throw new NotFoundException(String.format("Not found any agent hosts (yet) for cluster '%s'", cluster.getId()));
        }
        String agentHosts = agentHostsSet.stream()
                .map(InstanceMetaData::getDiscoveryFQDN)
                .collect(Collectors.joining("\n"));

        List<String> hostGroupHostsStrings = agentHostsSet.stream()
                .collect(Collectors.groupingBy(InstanceMetaData::getInstanceGroupName))
                .entrySet().stream()
                .map(s -> addSectionWithBody(
                        s.getKey(),
                        s.getValue().stream().map(InstanceMetaData::getDiscoveryFQDN).collect(Collectors.joining("\n"))))
                .collect(Collectors.toList());

        return String.join(
                "\n",
                addSectionWithBody("cluster", "name=" + clusterName),
                addSectionWithBody("server", serverHost),
                String.join("\n", hostGroupHostsStrings),
                addSectionWithBody("agent", agentHosts),
                addSectionWithBody("all:vars", String.format("ansible_ssh_user=%s", loginUser))
        );
    }

    private String addSectionWithBody(String section, String rawBody) {
        return String.format("[%s]%n%s%n", section, rawBody);
    }

    private void saveAndFireEventOnClusterStatusChange(Cluster cluster, Long stackId, Status status, ResourceEvent event) {
        if (!status.equals(cluster.getStatus())) {
            cluster.setStatus(status);
            clusterService.save(cluster);
            cloudbreakEventService.fireCloudbreakEvent(stackId, event.name(), messagesService.getMessage(event.getMessage()));
        }
    }
}
