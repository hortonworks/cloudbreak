package com.sequenceiq.cloudbreak.service;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.HostGroupDecorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class ClusterCommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCommonService.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private HostGroupDecorator hostGroupDecorator;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private HostGroupService hostGroupService;

    @Autowired
    private BlueprintValidator blueprintValidator;

    @Autowired
    private StackService stackService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private ClusterCreationSetupService clusterCreationSetupService;

    public Response put(Long stackId, UpdateClusterJson updateJson) {
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        UserNamePasswordJson userNamePasswordJson = updateJson.getUserNamePasswordJson();
        if (userNamePasswordJson != null) {
            ambariUserNamePasswordChange(stackId, stack, userNamePasswordJson);
            return Response.status(Status.NO_CONTENT).build();
        }

        if (updateJson.getStatus() != null) {
            LOGGER.info("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            clusterService.updateStatus(stackId, updateJson.getStatus());
            return Response.status(Status.NO_CONTENT).build();
        }

        if (updateJson.getBlueprintId() != null && updateJson.getHostgroups() != null && stack.getCluster().isCreateFailed()) {
            LOGGER.info("Cluster rebuild request received. Stack id:  {}", stackId);
            try {
                recreateCluster(stackId, updateJson);
            } catch (TransactionExecutionException e) {
                throw new TransactionRuntimeExecutionException(e);
            }
            return Response.status(Status.NO_CONTENT).build();
        }

        if (updateJson.getHostGroupAdjustment() != null) {
            clusterHostgroupAdjustmentChange(stackId, updateJson, stack);
            return Response.status(Status.NO_CONTENT).build();
        }
        LOGGER.error("Invalid cluster update request received. Stack id: {}", stackId);
        throw new BadRequestException("Invalid update cluster request!");
    }

    private void clusterHostgroupAdjustmentChange(Long stackId, UpdateClusterJson updateJson, Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster host adjustment request received. Stack id: {} ", stackId);
        Blueprint blueprint = stack.getCluster().getBlueprint();
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), updateJson.getHostGroupAdjustment().getHostGroup());
        if (hostGroup == null) {
            throw new BadRequestException(String.format("Host group '%s' not found or not member of the cluster '%s'",
                    updateJson.getHostGroupAdjustment().getHostGroup(), stack.getName()));
        }
        blueprintValidator.validateHostGroupScalingRequest(blueprint, hostGroup, updateJson.getHostGroupAdjustment().getScalingAdjustment());
        clusterService.updateHosts(stackId, updateJson.getHostGroupAdjustment());
    }

    private void recreateCluster(Long stackId, UpdateClusterJson updateJson) throws TransactionExecutionException {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupRequest json : updateJson.getHostgroups()) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup = hostGroupDecorator.decorate(hostGroup, json, user, stackId, false, false);
            hostGroups.add(hostGroup);
        }
        AmbariStackDetailsJson stackDetails = updateJson.getAmbariStackDetails();
        StackRepoDetails stackRepoDetails = null;
        if (stackDetails != null) {
            stackRepoDetails = conversionService.convert(stackDetails, StackRepoDetails.class);
        }
        clusterService.recreate(stackId, updateJson.getBlueprintId(), hostGroups, updateJson.getValidateBlueprint(), stackRepoDetails,
                updateJson.getKerberosPassword(), updateJson.getKerberosPrincipal());
    }

    private void ambariUserNamePasswordChange(Long stackId, Stack stack, UserNamePasswordJson userNamePasswordJson) {
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
        LOGGER.info("Cluster username password update request received. Stack id:  {}, username: {}",
                stackId, userNamePasswordJson.getUserName());
        clusterService.updateUserNamePassword(stackId, userNamePasswordJson);
    }
}
