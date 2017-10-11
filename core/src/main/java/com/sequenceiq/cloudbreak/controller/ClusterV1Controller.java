package com.sequenceiq.cloudbreak.controller;

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
import com.sequenceiq.cloudbreak.api.model.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Controller
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

    @Override
    public ClusterResponse post(Long stackId, ClusterRequest request) throws Exception {
        IdentityUser user = authenticatedUserService.getCbUser();

        Stack stack = stackService.getById(stackId);

        clusterCreationSetupService.validate(request, stack, user);
        Cluster cluster = clusterCreationSetupService.prepare(request, stack, user);

        return conversionService.convert(cluster, ClusterResponse.class);
    }

    @Override
    public ClusterResponse get(Long stackId) {
        Stack stack = stackService.get(stackId);
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
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPrivateStack(name, user);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stack.getId(), ClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public ClusterResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.getPublicStack(name, user);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stack.getId(), ClusterResponse.class);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public void delete(Long stackId) {
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(stackId);
    }

    @Override
    public Response put(Long stackId, UpdateClusterJson updateJson) throws CloudbreakSecuritySetupException {
        return clusterCommonService.put(stackId, updateJson);
    }

    @Override
    public ConfigsResponse getConfigs(Long stackId, ConfigsRequest requests) throws Exception {
        return clusterService.retrieveOutputs(stackId, requests.getRequests());
    }

    @Override
    public Response upgradeCluster(Long stackId, AmbariRepoDetailsJson ambariRepoDetails) {
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        AmbariRepo ambariRepo = conversionService.convert(ambariRepoDetails, AmbariRepo.class);
        clusterService.upgrade(stackId, ambariRepo);
        return Response.accepted().build();
    }

    @Override
    public Response failureReport(Long stackId, FailureReport failureReport) throws CloudbreakSecuritySetupException {
        clusterService.failureReport(stackId, failureReport.getFailedNodes());
        return Response.accepted().build();
    }

    @Override
    public Response repairCluster(Long stackId, ClusterRepairRequest clusterRepairRequest) throws CloudbreakSecuritySetupException {
        clusterService.repairCluster(stackId, clusterRepairRequest.getHostGroups(), clusterRepairRequest.isRemoveOnly());
        return Response.accepted().build();
    }
}
