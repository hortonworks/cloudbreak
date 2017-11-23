package com.sequenceiq.cloudbreak.controller;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.BYOS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.Component;
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

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private ClusterDecorator clusterDecorator;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private DefaultHDPEntries defaultHDPEntries;

    @Inject
    private DefaultHDFEntries defaultHDFEntries;

    @Inject
    private BlueprintService blueprintService;

    public void validate(ClusterRequest request, Stack stack, IdentityUser user) {
        validate(request, null, stack, user);
    }

    public void validate(ClusterRequest request, CloudCredential cloudCredential, Stack stack, IdentityUser user) {
        if (request.getEnableSecurity() && request.getKerberos() == null) {
            throw new BadRequestException("If the security is enabled the kerberos parameters cannot be empty");
        }
        MDCBuilder.buildUserMdcContext(user);
        if (!stack.isAvailable() && BYOS.equals(stack.cloudPlatform())) {
            throw new BadRequestException("Stack is not in 'AVAILABLE' status, cannot create cluster now.");
        }
        CloudCredential credential = cloudCredential;
        if (credential == null) {
            credential = credentialToCloudCredentialConverter.convert(stack.getCredential());
        }
        fileSystemValidator.validateFileSystem(stack.cloudPlatform(), credential, request.getFileSystem());
    }

    public Cluster prepare(ClusterRequest request, Stack stack, IdentityUser user) throws Exception {
        return prepare(request, stack, null, user);
    }

    public Cluster prepare(ClusterRequest request, Stack stack, Blueprint blueprint, IdentityUser user) throws Exception {
        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster = clusterDecorator.decorate(cluster, request, blueprint, user, stack);
        List<ClusterComponent> components = new ArrayList<>();
        Set<Component> allComponent = componentConfigProvider.getAllComponentsByStackIdAndType(stack.getId(),
                Sets.newHashSet(ComponentType.AMBARI_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS));
        Optional<Component> stackAmbariRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.AMBARI_REPO_DETAILS)
                && c.getName().equalsIgnoreCase(ComponentType.AMBARI_REPO_DETAILS.name())).findAny();
        Optional<Component> stackHdpRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.HDP_REPO_DETAILS)
                && c.getName().equalsIgnoreCase(ComponentType.HDP_REPO_DETAILS.name())).findAny();
        Optional<Component> stackImageComponent = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.IMAGE)
                && c.getName().equalsIgnoreCase(ComponentType.IMAGE.name())).findAny();
        components = addAmbariRepoConfig(stackAmbariRepoConfig, components, request.getAmbariRepoDetailsJson(), cluster);
        components = addHDPRepoConfig(blueprint, stackHdpRepoConfig, components, request, cluster, user, stackImageComponent);
        components = addAmbariDatabaseConfig(components, request.getAmbariDatabaseDetails(), cluster);
        return clusterService.create(user, stack, cluster, components);
    }

    private List<ClusterComponent> addAmbariRepoConfig(Optional<Component> stackAmbariRepoConfig, List<ClusterComponent> components,
            AmbariRepoDetailsJson ambariRepoDetailsJson, Cluster cluster) throws JsonProcessingException {
        // If it is not predefined in image catalog
        if (!stackAmbariRepoConfig.isPresent()) {
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

    private List<ClusterComponent> addHDPRepoConfig(Blueprint blueprint, Optional<Component> stackHdpRepoConfig,
            List<ClusterComponent> components, ClusterRequest request, Cluster cluster, IdentityUser user, Optional<Component> stackImageComponent)
            throws JsonProcessingException {

        Json stackRepoDetailsJson;
        if (!stackHdpRepoConfig.isPresent()) {
            if (request.getAmbariStackDetails() != null) {
                StackRepoDetails stackRepoDetails = conversionService.convert(request.getAmbariStackDetails(), StackRepoDetails.class);
                stackRepoDetailsJson = new Json(stackRepoDetails);
            } else {
                StackRepoDetails repo = defaultHDPInfo(blueprint, request, user).getRepo();
                pruneVDFUrlsByOsType(cluster, stackImageComponent, repo);
                stackRepoDetailsJson = new Json(repo);
            }
        } else {
            stackRepoDetailsJson = stackHdpRepoConfig.get().getAttributes();
        }
        ClusterComponent hdpRepoComponent = new ClusterComponent(ComponentType.HDP_REPO_DETAILS, stackRepoDetailsJson, cluster);
        components.add(hdpRepoComponent);
        return components;
    }

    private StackInfo defaultHDPInfo(Blueprint blueprint, ClusterRequest request, IdentityUser user) {
        try {
            JsonNode root = getBlueprintJsonNode(blueprint, request, user);
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

    private JsonNode getBlueprintJsonNode(Blueprint blueprint, ClusterRequest request, IdentityUser user) throws IOException {
        JsonNode root;
        if (blueprint != null) {
            root = JsonUtil.readTree(blueprint.getBlueprintText());
        } else {
            // Backward compatibility to V1 cluster API
            if (request.getBlueprintId() != null) {
                root = JsonUtil.readTree(blueprintService.get(request.getBlueprintId()).getBlueprintText());
            } else if (request.getBlueprintName() != null) {
                root = JsonUtil.readTree(blueprintService.get(request.getBlueprintName(), user.getAccount()).getBlueprintText());
            } else {
                root = JsonUtil.readTree(request.getBlueprint().getAmbariBlueprint());
            }
        }
        return root;
    }

    private void pruneVDFUrlsByOsType(Cluster cluster, Optional<Component> stackImageComponent, StackRepoDetails repo) {
        if (stackImageComponent.isPresent()) {
            try {
                Image imageComponent = stackImageComponent.get().getAttributes().get(Image.class);
                if (!StringUtils.isEmpty(stackImageComponent)) {
                    String osType = imageComponent.getOsType();
                    Set<String> vdfUrlKeysToRemove = repo.getStack().keySet().stream()
                            .filter(key -> key.startsWith(StackRepoDetails.VDF_REPO_KEY_PREFIX))
                            .filter(key -> !StringUtils.isEmpty(osType) && !key.equalsIgnoreCase(StackRepoDetails.VDF_REPO_KEY_PREFIX + osType))
                            .collect(Collectors.toSet());

                    vdfUrlKeysToRemove.forEach(key -> repo.getStack().remove(key));
                }
            } catch (IOException e) {
                LOGGER.warn("Could not get Image component from database for cluster: " + cluster.getId(), e);
            }
        }
    }

    private List<ClusterComponent> addAmbariDatabaseConfig(List<ClusterComponent> components, AmbariDatabaseDetailsJson ambariRepoDetailsJson, Cluster cluster)
            throws JsonProcessingException {
        if (ambariRepoDetailsJson == null) {
            ambariRepoDetailsJson = new AmbariDatabaseDetailsJson();
        }
        AmbariDatabase ambariDatabase = conversionService.convert(ambariRepoDetailsJson, AmbariDatabase.class);
        ClusterComponent component = new ClusterComponent(ComponentType.AMBARI_DATABASE_DETAILS, new Json(ambariDatabase), cluster);
        components.add(component);
        return components;
    }

}