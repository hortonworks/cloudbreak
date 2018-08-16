package com.sequenceiq.cloudbreak.controller;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.UpdateGatewayTopologiesJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.ClusterCommonService;
import com.sequenceiq.cloudbreak.service.ClusterCreationSetupService;
import com.sequenceiq.cloudbreak.service.StackCommonService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Controller
@Transactional(TxType.NEVER)
public class ClusterV1Controller implements ClusterV1Endpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV1Controller.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private ClusterCommonService clusterCommonService;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private StackService stackService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private ClusterCreationSetupService clusterCreationSetupService;

    @Autowired
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private StackCommonService stackCommonService;

    @Autowired
    private OrganizationService organizationService;

    @Override
    public ClusterResponse post(Long stackId, ClusterRequest request) throws Exception {
        IdentityUser user = authenticatedUserService.getCbUser();

        Stack stack = stackService.getByIdWithListsWithoutAuthorization(stackId);

        clusterCreationSetupService.validate(request, stack, user);
        Cluster cluster = clusterCreationSetupService.prepare(request, stack, user);
        Optional<StackInputs> stackInputs = sharedServiceConfigProvider.prepareDatalakeConfigs(cluster.getBlueprint(), stack);
        if (stackInputs.isPresent()) {
            sharedServiceConfigProvider.updateStackinputs(stackInputs.get(), stack);
        }
        return conversionService.convert(cluster, ClusterResponse.class);
    }

    @Override
    public ClusterResponse get(Long stackId) {
        Stack stack = stackService.getById(stackId);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stackId, ClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public AutoscaleClusterResponse getForAutoscale(Long stackId) {
        Stack stack = stackService.getForAutoscale(stackId);
        AutoscaleClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stackId, AutoscaleClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public ClusterResponse getPrivate(String name) {
        Stack stack = stackService.getByNameInDefaultOrg(name);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stack.getId(), ClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public ClusterResponse getPublic(String name) {
        Stack stack = stackService.getByNameInDefaultOrg(name);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stack.getId(), ClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public void delete(Long stackId, Boolean withStackDelete, Boolean deleteDependencies) {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stackId, withStackDelete, deleteDependencies);
    }

    @Override
    public Response put(Long stackId, UpdateClusterJson updateJson) {
        return clusterCommonService.put(stackId, updateJson);
    }

    @Override
    public ConfigsResponse getConfigs(Long stackId, ConfigsRequest requests) throws Exception {
        return clusterService.retrieveOutputs(stackId, requests.getRequests());
    }

    @Override
    public Response upgradeCluster(Long stackId, AmbariRepoDetailsJson ambariRepoDetails) {
        Stack stack = stackService.getById(stackId);
        MDCBuilder.buildMdcContext(stack);
        AmbariRepo ambariRepo = conversionService.convert(ambariRepoDetails, AmbariRepo.class);
        try {
            clusterService.upgrade(stackId, ambariRepo);
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        return Response.accepted().build();
    }

    @Override
    public Response failureReport(Long stackId, FailureReport failureReport) {
        clusterService.failureReport(stackId, failureReport.getFailedNodes());
        return Response.accepted().build();
    }

    @Override
    public Response repairCluster(Long stackId, ClusterRepairRequest clusterRepairRequest) {
        clusterService.repairCluster(stackId, clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
        return Response.accepted().build();
    }

    @Override
    public GatewayJson updateGatewayTopologies(Long stackId, UpdateGatewayTopologiesJson request) {
        return gatewayService.updateGatewayTopologies(stackId, request);
    }
}
