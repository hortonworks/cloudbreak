package com.sequenceiq.cloudbreak.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Controller
public class ClusterController implements ClusterEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private Decorator<Cluster> clusterDecorator;

    @Autowired
    private Decorator<HostGroup> hostGroupDecorator;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private HostGroupService hostGroupService;

    @Autowired
    private BlueprintValidator blueprintValidator;

    @Autowired
    private FileSystemValidator fileSystemValidator;

    @Autowired
    private RdsConnectionValidator rdsConnectionValidator;

    @Autowired
    private StackService stackService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private SssdConfigService sssdConfigService;

    @Autowired
    private RdsConfigService rdsConfigService;

    @Autowired
    private ComponentConfigProvider componentConfigProvider;

    @Override
    public IdJson post(Long stackId, ClusterRequest request) throws Exception {
        CbUser user = authenticatedUserService.getCbUser();
        if (request.getEnableSecurity()
                && (request.getKerberosMasterKey() == null || request.getKerberosAdmin() == null || request.getKerberosPassword() == null)) {
            throw new BadRequestException("If the security is enabled the kerberos parameters cannot be empty");
        }
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getById(stackId);
        if (!stack.isAvailable() && CloudConstants.BYOS.equals(stack.cloudPlatform())) {
            throw new BadRequestException("Stack is not in 'AVAILABLE' status, cannot create cluster now.");
        }
        fileSystemValidator.validateFileSystem(stack.cloudPlatform(), request.getFileSystem());
        validateRdsConfigParams(request);
        if (request.getRdsConfigJson() != null) {
            validateRdsConnection(request);
            RDSConfig rdsConfig = rdsConfigService.create(user, conversionService.convert(request.getRdsConfigJson(), RDSConfig.class));
            request.setRdsConfigId(rdsConfig.getId());
        }
        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster = clusterDecorator.decorate(cluster, stackId, user, request.getBlueprintId(), request.getHostGroups(), request.getValidateBlueprint(),
                request.getSssdConfigId(), request.getRdsConfigId(), request.getLdapConfigId());
        if (cluster.isLdapRequired() && cluster.getSssdConfig() == null) {
            cluster.setSssdConfig(sssdConfigService.getDefaultSssdConfig(user));
        }
        List<Component> components = new ArrayList<>();
        components = addAmbariRepoConfig(components, request, stack);
        components = addHDPRepoConfig(components, request, stack);
        components = addAmbariDatabaseConfig(components, request, stack);
        clusterService.create(user, stackId, cluster, components);
        return new IdJson(cluster.getId());
    }

    private void validateRdsConnection(ClusterRequest request) {
        if (request.getRdsConfigJson().isValidated()) {
            rdsConnectionValidator.validateRdsConnection(request.getRdsConfigJson());
        }
    }

    @Override
    public ClusterResponse get(Long stackId) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.get(stackId);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stackId);
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stackId);
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public ClusterResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPrivateStack(name, user);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public ClusterResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getPublicStack(name, user);
        ClusterResponse cluster = clusterService.retrieveClusterForCurrentUser(stack.getId());
        String clusterJson = clusterService.getClusterJson(stack.getAmbariIp(), stack.getId());
        return clusterService.getClusterResponse(cluster, clusterJson);
    }

    @Override
    public void delete(Long stackId) {
        CbUser user = authenticatedUserService.getCbUser();
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        clusterService.delete(user, stackId);
    }

    @Override
    public Response put(Long stackId, UpdateClusterJson updateJson) throws CloudbreakSecuritySetupException {
        Stack stack = stackService.get(stackId);
        MDCBuilder.buildMdcContext(stack);
        UserNamePasswordJson userNamePasswordJson = updateJson.getUserNamePasswordJson();
        if (userNamePasswordJson != null) {
            ambariUserNamePasswordChange(stackId, stack, userNamePasswordJson);
            return Response.status(Response.Status.ACCEPTED).build();
        }

        if (updateJson.getStatus() != null) {
            LOGGER.info("Cluster status update request received. Stack id:  {}, status: {} ", stackId, updateJson.getStatus());
            clusterService.updateStatus(stackId, updateJson.getStatus());
            return Response.status(Response.Status.ACCEPTED).build();
        }

        if (updateJson.getBlueprintId() != null && updateJson.getHostgroups() != null && stack.getCluster().isCreateFailed()) {
            LOGGER.info("Cluster rebuild request received. Stack id:  {}", stackId);
            recreateCluster(stackId, updateJson);
            return Response.status(Response.Status.ACCEPTED).build();
        }

        if (updateJson.getHostGroupAdjustment() != null) {
            clusterHostgroupAdjustmentChange(stackId, updateJson, stack);
            return Response.status(Response.Status.ACCEPTED).build();
        }
        LOGGER.error("Invalid cluster update request received. Stack id: {}", stackId);
        throw new BadRequestException("Invalid update cluster request!");
    }

    @Override
    public ConfigsResponse getConfigs(Long stackId, ConfigsRequest requests) throws Exception {
        CbUser user = authenticatedUserService.getCbUser();
        MDCBuilder.buildUserMdcContext(user);
        return clusterService.retrieveOutputs(stackId, requests.getRequests());
    }

    private List<Component> addAmbariRepoConfig(List<Component> components, ClusterRequest request, Stack stack) throws JsonProcessingException {
        // If it is not predefined in image catalog
        if (componentConfigProvider.getAmbariRepo(stack.getId()) == null) {
            AmbariRepoDetailsJson ambariRepoDetailsJson = request.getAmbariRepoDetailsJson();
            if (ambariRepoDetailsJson == null) {
                ambariRepoDetailsJson = new AmbariRepoDetailsJson();
            }
            AmbariRepo ambariRepo = conversionService.convert(ambariRepoDetailsJson, AmbariRepo.class);
            Component component = new Component(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(), new Json(ambariRepo), stack);
            components.add(component);
        }
        return components;
    }

    private List<Component> addHDPRepoConfig(List<Component> components, ClusterRequest request, Stack stack) throws JsonProcessingException {
        if (componentConfigProvider.getHDPRepo(stack.getId()) == null) {
            AmbariStackDetailsJson ambariStackDetailsJson = request.getAmbariStackDetails();
            if (ambariStackDetailsJson != null) {
                HDPRepo hdpRepo = conversionService.convert(ambariStackDetailsJson, HDPRepo.class);
                Component component = new Component(ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name(), new Json(hdpRepo), stack);
                components.add(component);
            }
        }
        return components;
    }

    private List<Component> addAmbariDatabaseConfig(List<Component> components, ClusterRequest request, Stack stack) throws JsonProcessingException {
        AmbariDatabaseDetailsJson ambariRepoDetailsJson = request.getAmbariDatabaseDetails();
        if (ambariRepoDetailsJson == null) {
            ambariRepoDetailsJson = new AmbariDatabaseDetailsJson();
        }
        AmbariDatabase ambariDatabase = conversionService.convert(ambariRepoDetailsJson, AmbariDatabase.class);
        Component component = new Component(ComponentType.AMBARI_DATABASE_DETAILS, ComponentType.AMBARI_DATABASE_DETAILS.name(), new Json(ambariDatabase),
                stack);
        components.add(component);
        return components;
    }

    private void clusterHostgroupAdjustmentChange(Long stackId, UpdateClusterJson updateJson, Stack stack)
            throws CloudbreakSecuritySetupException {
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

    private void recreateCluster(Long stackId, UpdateClusterJson updateJson) {
        CbUser user = authenticatedUserService.getCbUser();
        Set<HostGroup> hostGroups = new HashSet<>();
        for (HostGroupJson json : updateJson.getHostgroups()) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup = hostGroupDecorator.decorate(hostGroup, stackId, user, json.getConstraint(), json.getRecipeIds(), false);
            hostGroups.add(hostGroup);
        }
        AmbariStackDetailsJson stackDetails = updateJson.getAmbariStackDetails();
        HDPRepo hdpRepo = null;
        if (stackDetails != null) {
            hdpRepo = conversionService.convert(stackDetails, HDPRepo.class);
        }
        clusterService.recreate(stackId, updateJson.getBlueprintId(), hostGroups, updateJson.getValidateBlueprint(), hdpRepo);
    }

    private void ambariUserNamePasswordChange(Long stackId, Stack stack, UserNamePasswordJson userNamePasswordJson) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(String.format(
                    "Stack '%s' is currently in '%s' state. PUT requests to a cluster can only be made if the underlying stack is 'AVAILABLE'.", stackId,
                    stack.getStatus()));
        }
        if (Optional.ofNullable(userNamePasswordJson.getOldPassword()).orElse("").isEmpty()) {
            throw new BadRequestException(String.format("The old password of the Stack '%s' is missing.", stackId));
        } else if (Optional.ofNullable(userNamePasswordJson.getPassword()).orElse("").isEmpty()) {
            throw new BadRequestException(String.format("The new password of the Stack '%s' is missing.", stackId));
        }
        if (!userNamePasswordJson.getOldPassword().equals(stack.getCluster().getPassword())) {
            throw new BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password on Stack '%s' with status '%s'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster username password update request received. Stack id:  {}, username: {}, password: {}",
                stackId, userNamePasswordJson.getUserName(), userNamePasswordJson.getPassword());
        clusterService.updateUserNamePassword(stackId, userNamePasswordJson);
    }

    private void validateRdsConfigParams(ClusterRequest request) {
        if (request.getRdsConfigJson() != null && request.getRdsConfigId() != null) {
            throw new BadRequestException("Both rdsConfig and rdsConfigId cannot be set in the same request.");
        }
    }
}
