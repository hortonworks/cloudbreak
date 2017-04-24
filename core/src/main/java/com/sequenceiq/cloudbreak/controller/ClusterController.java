package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.FailureReport;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.DefaultHDPInfos;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.Decorator;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sssdconfig.SssdConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

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
    private BlueprintService blueprintService;

    @Autowired
    private BlueprintUtils blueprintUtils;

    @Autowired
    private RdsConfigService rdsConfigService;

    @Autowired
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Autowired
    private ComponentConfigProvider componentConfigProvider;

    @Autowired
    private DefaultHDPInfos defaultHDPInfos;

    @Autowired
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Override
    public ClusterResponse post(Long stackId, ClusterRequest request) throws Exception {
        CbUser user = authenticatedUserService.getCbUser();
        if (request.getEnableSecurity() && request.getKerberos() == null) {
            throw new BadRequestException("If the security is enabled the kerberos parameters cannot be empty");
        }
        MDCBuilder.buildUserMdcContext(user);
        Stack stack = stackService.getById(stackId);
        if (!stack.isAvailable() && BYOS.equals(stack.cloudPlatform())) {
            throw new BadRequestException("Stack is not in 'AVAILABLE' status, cannot create cluster now.");
        }
        Credential credential = stack.getCredential();
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

        fileSystemValidator.validateFileSystem(stack.cloudPlatform(), cloudCredential, request.getFileSystem());
        validateRdsConfigParams(request);
        if (request.getRdsConfigJsons() != null && !request.getRdsConfigJsons().isEmpty()) {
            for (RDSConfigJson rdsConfigJson : request.getRdsConfigJsons()) {
                validateRdsConnection(rdsConfigJson);
                RDSConfig rdsConfig = rdsConfigService.create(user, conversionService.convert(rdsConfigJson, RDSConfig.class));
                request.getRdsConfigIds().add(rdsConfig.getId());
            }


        }
        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster = clusterDecorator.decorate(cluster, stackId, user,
                request.getBlueprintId(), request.getHostGroups(), request.getValidateBlueprint(),
                request.getSssdConfigId(), request.getRdsConfigIds(), request.getLdapConfigId(),
                request.getBlueprint(), request.getSssdConfig(), request.getRdsConfigJsons(), request.getLdapConfig());
        if (cluster.isLdapRequired() && cluster.getSssdConfig() == null) {
            cluster.setSssdConfig(sssdConfigService.getDefaultSssdConfig(user));
        }
        List<ClusterComponent> components = new ArrayList<>();
        components = addAmbariRepoConfig(stackId, components, request, cluster);
        components = addHDPRepoConfig(stackId, components, request, cluster);
        components = addAmbariDatabaseConfig(components, request, cluster);
        Cluster resp = clusterService.create(user, stackId, cluster, components);
        return conversionService.convert(resp, ClusterResponse.class);
    }

    private void validateRdsConnection(RDSConfigJson request) {
        if (request.isValidated()) {
            rdsConnectionValidator.validateRdsConnection(request);
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

    private List<ClusterComponent> addAmbariRepoConfig(Long stackId, List<ClusterComponent> components, ClusterRequest request, Cluster cluster)
            throws JsonProcessingException {
        // If it is not predefined in image catalog
        Component stackAmbariRepoConfig = componentConfigProvider.getComponent(stackId, ComponentType.AMBARI_REPO_DETAILS,
                ComponentType.AMBARI_REPO_DETAILS.name());
        if (stackAmbariRepoConfig == null) {
            AmbariRepoDetailsJson ambariRepoDetailsJson = request.getAmbariRepoDetailsJson();
            if (ambariRepoDetailsJson == null) {
                ambariRepoDetailsJson = new AmbariRepoDetailsJson();
            }
            AmbariRepo ambariRepo = conversionService.convert(ambariRepoDetailsJson, AmbariRepo.class);
            ClusterComponent component = new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(), new Json(ambariRepo),
                    cluster);
            components.add(component);
        }  else {
            ClusterComponent ambariRepo = new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(),
                    stackAmbariRepoConfig.getAttributes(), cluster);
            components.add(ambariRepo);
        }
        return components;
    }

    private List<ClusterComponent> addHDPRepoConfig(Long stackId, List<ClusterComponent> components, ClusterRequest request, Cluster cluster)
            throws JsonProcessingException {
        Component stackHdpRepoConfig = componentConfigProvider.getComponent(stackId, ComponentType.HDP_REPO_DETAILS,
                ComponentType.HDP_REPO_DETAILS.name());
        if (stackHdpRepoConfig == null) {
            AmbariStackDetailsJson ambariStackDetailsJson = request.getAmbariStackDetails();
            if (ambariStackDetailsJson != null) {
                HDPRepo hdpRepo = conversionService.convert(ambariStackDetailsJson, HDPRepo.class);
                ClusterComponent component = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name(), new Json(hdpRepo),
                        cluster);
                components.add(component);
            } else {
                ClusterComponent hdpRepoComponent = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name(),
                        new Json(defaultHDPInfo(request).getRepo()), cluster);
                components.add(hdpRepoComponent);
            }
        } else {
            ClusterComponent hdpRepoComponent = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS.name(),
                    stackHdpRepoConfig.getAttributes(), cluster);
            components.add(hdpRepoComponent);
        }
        return components;
    }

    private DefaultHDPInfo defaultHDPInfo(ClusterRequest clusterRequest) {
        try {
            JsonNode root = null;
            if (clusterRequest.getBlueprintId() != null) {
                Blueprint blueprint = blueprintService.get(clusterRequest.getBlueprintId());
                root = JsonUtil.readTree(blueprint.getBlueprintText());
            } else {
                root = JsonUtil.readTree(clusterRequest.getBlueprint().getAmbariBlueprint());

            }
            if (root != null) {
                String blueprintHdpVersion = blueprintUtils.getBlueprintHdpVersion(root);
                for (Map.Entry<String, DefaultHDPInfo> entry: defaultHDPInfos.getEntries().entrySet()) {
                    if (entry.getKey().equals(blueprintHdpVersion)) {
                        return entry.getValue();
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Can not initiate default hdp info: ", ex);
        }
        return defaultHDPInfos.getEntries().values().iterator().next();
    }

    private List<ClusterComponent> addAmbariDatabaseConfig(List<ClusterComponent> components, ClusterRequest request, Cluster cluster)
            throws JsonProcessingException {
        AmbariDatabaseDetailsJson ambariRepoDetailsJson = request.getAmbariDatabaseDetails();
        if (ambariRepoDetailsJson == null) {
            ambariRepoDetailsJson = new AmbariDatabaseDetailsJson();
        }
        AmbariDatabase ambariDatabase = conversionService.convert(ambariRepoDetailsJson, AmbariDatabase.class);
        ClusterComponent component = new ClusterComponent(ComponentType.AMBARI_DATABASE_DETAILS, ComponentType.AMBARI_DATABASE_DETAILS.name(),
                new Json(ambariDatabase), cluster);
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
        for (HostGroupRequest json : updateJson.getHostgroups()) {
            HostGroup hostGroup = conversionService.convert(json, HostGroup.class);
            hostGroup = hostGroupDecorator.decorate(hostGroup, stackId, user, json.getConstraint(), json.getRecipeIds(), false, json.getRecipes(),
                    false);
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
        if (!userNamePasswordJson.getOldPassword().equals(stack.getCluster().getPassword())) {
            throw new BadRequestException(String.format(
                    "Cluster actual password does not match in the request, please pass the real password on Stack '%s' with status '%s'.", stackId,
                    stack.getStatus()));
        }
        LOGGER.info("Cluster username password update request received. Stack id:  {}, username: {}",
                stackId, userNamePasswordJson.getUserName());
        clusterService.updateUserNamePassword(stackId, userNamePasswordJson);
    }

    private void validateRdsConfigParams(ClusterRequest request) {
        if (request.getRdsConfigJsons() != null && !request.getRdsConfigJsons().isEmpty()
                && request.getRdsConfigIds() != null && !request.getRdsConfigIds().isEmpty()) {
            throw new BadRequestException("Both rdsConfigs and rdsConfigIds cannot be set in the same request.");
        }
    }
}
