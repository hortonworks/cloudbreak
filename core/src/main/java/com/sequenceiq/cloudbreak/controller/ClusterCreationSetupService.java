package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@org.springframework.stereotype.Component
public class ClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSetupService.class);

    @Autowired
    private FileSystemValidator fileSystemValidator;

    @Autowired
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private ClusterDecorator clusterDecorator;

    @Autowired
    private ClusterService clusterService;

    @Autowired
    private ComponentConfigProvider componentConfigProvider;

    @Autowired
    private BlueprintUtils blueprintUtils;

    @Autowired
    private DefaultHDPEntries defaultHDPEntries;

    @Autowired
    private DefaultHDFEntries defaultHDFEntries;

    @Autowired
    private BlueprintService blueprintService;

    public void validate(ClusterRequest request, Stack stack, IdentityUser user) {
        if (request.getEnableSecurity() && request.getKerberos() == null) {
            throw new BadRequestException("If the security is enabled the kerberos parameters cannot be empty");
        }
        MDCBuilder.buildUserMdcContext(user);
        if (!stack.isAvailable() && BYOS.equals(stack.cloudPlatform())) {
            throw new BadRequestException("Stack is not in 'AVAILABLE' status, cannot create cluster now.");
        }
        Credential credential = stack.getCredential();
        CloudCredential cloudCredential = credentialToCloudCredentialConverter.convert(credential);

        fileSystemValidator.validateFileSystem(stack.cloudPlatform(), cloudCredential, request.getFileSystem());
    }

    public Cluster prepare(ClusterRequest request, Stack stack, IdentityUser user) throws Exception {
        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster = clusterDecorator.decorate(cluster, stack.getId(), user, request.getBlueprintId(), request.getHostGroups(), request.getValidateBlueprint(),
            request.getRdsConfigIds(), request.getLdapConfigId(), request.getBlueprint(), request.getRdsConfigJsons(), request.getLdapConfig(),
            request.getConnectedCluster(), request.getBlueprintName());
        List<ClusterComponent> components = new ArrayList<>();
        Set<Component> allComponent = componentConfigProvider.getAllComponentsByStackIdAndType(stack.getId(),
            Sets.newHashSet(ComponentType.AMBARI_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS));
        Optional<Component> stackAmbariRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.AMBARI_REPO_DETAILS)
            && c.getName().equalsIgnoreCase(ComponentType.AMBARI_REPO_DETAILS.name())).findAny();
        Optional<Component> stackHdpRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.HDP_REPO_DETAILS)
            && c.getName().equalsIgnoreCase(ComponentType.HDP_REPO_DETAILS.name())).findAny();
        components = addAmbariRepoConfig(stackAmbariRepoConfig, components, request, cluster);
        components = addHDPRepoConfig(stackHdpRepoConfig, components, request, cluster, user);
        components = addAmbariDatabaseConfig(components, request, cluster);
        return clusterService.create(user, stack, cluster, components);
    }

    private List<ClusterComponent> addAmbariRepoConfig(Optional<Component> stackAmbariRepoConfig, List<ClusterComponent> components,
        ClusterRequest request, Cluster cluster) throws JsonProcessingException {
        // If it is not predefined in image catalog
        if (!stackAmbariRepoConfig.isPresent()) {
            AmbariRepoDetailsJson ambariRepoDetailsJson = request.getAmbariRepoDetailsJson();
            if (ambariRepoDetailsJson == null) {
                ambariRepoDetailsJson = new AmbariRepoDetailsJson();
            }
            AmbariRepo ambariRepo = conversionService.convert(ambariRepoDetailsJson, AmbariRepo.class);
            ClusterComponent component = new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, new Json(ambariRepo), cluster);
            components.add(component);
        } else {
            ClusterComponent ambariRepo = new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, stackAmbariRepoConfig.get().getAttributes(), cluster);
            components.add(ambariRepo);
        }
        return components;
    }

    private List<ClusterComponent> addHDPRepoConfig(Optional<Component> stackHdpRepoConfig,
        List<ClusterComponent> components, ClusterRequest request, Cluster cluster, IdentityUser user) throws JsonProcessingException {
        if (!stackHdpRepoConfig.isPresent()) {
            AmbariStackDetailsJson ambariStackDetailsJson = request.getAmbariStackDetails();
            if (ambariStackDetailsJson != null) {
                HDPRepo hdpRepo = conversionService.convert(ambariStackDetailsJson, HDPRepo.class);
                ClusterComponent component = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, new Json(hdpRepo), cluster);
                components.add(component);
            } else {
                ClusterComponent hdpRepoComponent = new ClusterComponent(ComponentType.HDP_REPO_DETAILS,
                    new Json(defaultHDPInfo(request, user).getRepo()), cluster);
                components.add(hdpRepoComponent);
            }
        } else {
            ClusterComponent hdpRepoComponent = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, stackHdpRepoConfig.get().getAttributes(), cluster);
            components.add(hdpRepoComponent);
        }
        return components;
    }

    private HDPInfo defaultHDPInfo(ClusterRequest clusterRequest, IdentityUser user) {
        try {
            JsonNode root;
            if (clusterRequest.getBlueprintId() != null) {
                Blueprint blueprint = blueprintService.get(clusterRequest.getBlueprintId());
                root = JsonUtil.readTree(blueprint.getBlueprintText());
            } else if (clusterRequest.getBlueprintName() != null) {
                Blueprint blueprint = blueprintService.get(clusterRequest.getBlueprintName(), user.getAccount());
                root = JsonUtil.readTree(blueprint.getBlueprintText());
            } else {
                root = JsonUtil.readTree(clusterRequest.getBlueprint().getAmbariBlueprint());
            }
            if (root != null) {
                String stackVersion = blueprintUtils.getBlueprintHdpVersion(root);
                String stackName = blueprintUtils.getBlueprintStackName(root);
                if ("HDF".equalsIgnoreCase(stackName)) {
                    LOGGER.info("Stack name is HDF, use the default HDF repo for version: " + stackVersion);
                    for (Entry<String, DefaultHDFInfo> entry : defaultHDFEntries.getEntries().entrySet()) {
                        if (entry.getKey().equals(stackVersion)) {
                            return entry.getValue();
                        }
                    }
                } else {
                    LOGGER.info("Stack name is HDP, use the default HDP repo for version: " + stackVersion);
                    for (Entry<String, DefaultHDPInfo> entry : defaultHDPEntries.getEntries().entrySet()) {
                        if (entry.getKey().equals(stackVersion)) {
                            return entry.getValue();
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.warn("Can not initiate default hdp info: ", ex);
        }
        return defaultHDPEntries.getEntries().values().iterator().next();
    }

    private List<ClusterComponent> addAmbariDatabaseConfig(List<ClusterComponent> components, ClusterRequest request, Cluster cluster)
        throws JsonProcessingException {
        AmbariDatabaseDetailsJson ambariRepoDetailsJson = request.getAmbariDatabaseDetails();
        if (ambariRepoDetailsJson == null) {
            ambariRepoDetailsJson = new AmbariDatabaseDetailsJson();
        }
        AmbariDatabase ambariDatabase = conversionService.convert(ambariRepoDetailsJson, AmbariDatabase.class);
        ClusterComponent component = new ClusterComponent(ComponentType.AMBARI_DATABASE_DETAILS, new Json(ambariDatabase), cluster);
        components.add(component);
        return components;
    }

}
