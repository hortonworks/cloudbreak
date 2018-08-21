package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.CUSTOM_VDF_REPO_KEY;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.VDF_REPO_KEY_PREFIX;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_6_0_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.mpack.ManagementPackValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConfigValidator;
import com.sequenceiq.cloudbreak.converter.AmbariStackDetailsJsonToStackRepoDetailsConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService;
import com.sequenceiq.cloudbreak.service.decorator.ClusterDecorator;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class ClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationSetupService.class);

    @Inject
    private FileSystemValidator fileSystemValidator;

    @Inject
    private FileSystemConfigService fileSystemConfigService;

    @Inject
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private AmbariStackDetailsJsonToStackRepoDetailsConverter stackRepoDetailsConverter;

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

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Inject
    private ManagementPackValidator mpackValidator;

    @Inject
    private RdsConfigValidator rdsConfigValidator;

    public void validate(ClusterRequest request, Stack stack, IdentityUser user) {
        validate(request, null, stack, user);
    }

    public void validate(ClusterRequest request, CloudCredential cloudCredential, Stack stack, IdentityUser user) {
        if (request.getEnableSecurity() && request.getKerberos() == null) {
            throw new BadRequestException("If the security is enabled the kerberos parameters cannot be empty");
        }
        MDCBuilder.buildUserMdcContext(user);
        CloudCredential credential = cloudCredential;
        if (credential == null) {
            credential = credentialToCloudCredentialConverter.convert(stack.getCredential());
        }
        fileSystemValidator.validateFileSystem(stack.cloudPlatform(), credential, request.getFileSystem(),
                stack.getCreator().getUserId(), stack.getOrganization().getId());
        mpackValidator.validateMpacks(request, user);
        rdsConfigValidator.validateRdsConfigs(request);
    }

    public Cluster prepare(ClusterRequest request, Stack stack, IdentityUser user) throws CloudbreakImageNotFoundException,
            IOException, TransactionExecutionException {
        return prepare(request, stack, null, user);
    }

    public Cluster prepare(ClusterRequest request, Stack stack, Blueprint blueprint, IdentityUser user) throws IOException,
            CloudbreakImageNotFoundException, TransactionExecutionException {
        String stackName = stack.getName();

        long start = System.currentTimeMillis();

        if (request.getFileSystem() != null) {
            FileSystem fs = fileSystemConfigService.create(user, conversionService.convert(request.getFileSystem(), FileSystem.class), stack.getOrganization());
            request.getFileSystem().setName(fs.getName());
            LOGGER.info("File system saving took {} ms for stack {}", System.currentTimeMillis() - start, stackName);
        }

        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster.setStack(stack);
        LOGGER.info("Cluster conversion took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        start = System.currentTimeMillis();

        cluster = clusterDecorator.decorate(cluster, request, blueprint, stack.getOrganization(), stack);
        LOGGER.info("Cluster object decorated in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        start = System.currentTimeMillis();
        List<ClusterComponent> components = new ArrayList<>();
        Set<Component> allComponent = componentConfigProvider.getAllComponentsByStackIdAndType(stack.getId(),
                Sets.newHashSet(ComponentType.AMBARI_REPO_DETAILS, ComponentType.HDP_REPO_DETAILS, ComponentType.IMAGE));
        Optional<Component> stackAmbariRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.AMBARI_REPO_DETAILS)
                && c.getName().equalsIgnoreCase(ComponentType.AMBARI_REPO_DETAILS.name())).findAny();
        Optional<Component> stackHdpRepoConfig = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.HDP_REPO_DETAILS)
                && c.getName().equalsIgnoreCase(ComponentType.HDP_REPO_DETAILS.name())).findAny();
        Optional<Component> stackImageComponent = allComponent.stream().filter(c -> c.getComponentType().equals(ComponentType.IMAGE)
                && c.getName().equalsIgnoreCase(ComponentType.IMAGE.name())).findAny();
        ClusterComponent ambariRepoConfig = determineAmbariRepoConfig(stackAmbariRepoConfig, request.getAmbariRepoDetailsJson(), stackImageComponent, cluster);
        components.add(ambariRepoConfig);
        ClusterComponent hdpRepoConfig = determineHDPRepoConfig(blueprint, stack.getId(), stackHdpRepoConfig, request, cluster, stack.getOrganization(),
                stackImageComponent);
        components.add(hdpRepoConfig);

        checkVDFFile(ambariRepoConfig, hdpRepoConfig, stackName);

        LOGGER.info("Cluster components saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        start = System.currentTimeMillis();
        Cluster savedCluster = clusterService.create(user, stack, cluster, components);
        LOGGER.info("Cluster object creation took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        return savedCluster;
    }

    private void checkVDFFile(ClusterComponent ambariRepoConfig, ClusterComponent hdpRepoConfig, String stackName) throws IOException {
        AmbariRepo ambariRepo = ambariRepoConfig.getAttributes().get(AmbariRepo.class);

        if (ambariRepositoryVersionService.isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_6_0_0)
                && !containsVDFUrl(hdpRepoConfig.getAttributes())) {
            throw new BadRequestException(String.format("Couldn't determine any VDF file for the stack: %s", stackName));
        }
    }

    private ClusterComponent determineAmbariRepoConfig(Optional<Component> stackAmbariRepoConfig,
            AmbariRepoDetailsJson ambariRepoDetailsJson, Optional<Component> stackImageComponent, Cluster cluster) throws IOException {
        Json json;
        if (!stackAmbariRepoConfig.isPresent()) {
            JsonNode bluePrintJson = JsonUtil.readTree(cluster.getBlueprint().getBlueprintText());
            String stackVersion = blueprintUtils.getBlueprintStackVersion(bluePrintJson);
            String stackName = blueprintUtils.getBlueprintStackName(bluePrintJson);
            AmbariRepo ambariRepo = ambariRepoDetailsJson != null
                    ? conversionService.convert(ambariRepoDetailsJson, AmbariRepo.class)
                    : defaultAmbariRepoService.getDefault(getOsType(stackImageComponent), stackName, stackVersion);
            if (ambariRepo == null) {
                throw new BadRequestException(String.format("Couldn't determine Ambari repo for the stack: %s", cluster.getStack().getName()));
            }
            json = new Json(ambariRepo);
        } else {
            json = stackAmbariRepoConfig.get().getAttributes();
        }
        return new ClusterComponent(ComponentType.AMBARI_REPO_DETAILS, json, cluster);
    }

    private ClusterComponent determineHDPRepoConfig(Blueprint blueprint, long stackId, Optional<Component> stackHdpRepoConfig,
            ClusterRequest request, Cluster cluster, Organization organization, Optional<Component> stackImageComponent)
            throws IOException, CloudbreakImageNotFoundException {
        Json stackRepoDetailsJson;
        AmbariStackDetailsJson ambariStackDetails = request.getAmbariStackDetails();
        if (!stackHdpRepoConfig.isPresent()) {
            if (ambariStackDetails != null && (stackRepoDetailsConverter.isBaseRepoRequiredFieldsExists(ambariStackDetails)
                    || stackRepoDetailsConverter.isVdfRequiredFieldsExists(ambariStackDetails))) {
                setOsTypeFromImageIfMissing(cluster, stackImageComponent, ambariStackDetails);
                StackRepoDetails stackRepoDetails = stackRepoDetailsConverter.convert(ambariStackDetails);
                stackRepoDetailsJson = new Json(stackRepoDetails);
            } else {
                DefaultStackRepoDetails stackRepoDetails = SerializationUtils.clone(defaultHDPInfo(blueprint, request, organization).getRepo());
                String osType = getOsType(stackId);
                StackRepoDetails repo = createStackRepoDetails(stackRepoDetails, osType);
                Optional<String> vdfUrl = getVDFUrlByOsType(osType, stackRepoDetails);
                vdfUrl.ifPresent(s -> stackRepoDetails.getStack().put(CUSTOM_VDF_REPO_KEY, s));
                if (ambariStackDetails != null) {
                    repo.getMpacks().addAll(ambariStackDetails.getMpacks().stream().map(
                            rmpack -> conversionService.convert(rmpack, ManagementPackComponent.class)).collect(Collectors.toList()));
                }
                stackRepoDetailsJson = new Json(repo);
            }
        } else {
            stackRepoDetailsJson = stackHdpRepoConfig.get().getAttributes();
            if (ambariStackDetails != null && !ambariStackDetails.getMpacks().isEmpty()) {
                StackRepoDetails stackRepoDetails = stackRepoDetailsJson.get(StackRepoDetails.class);
                stackRepoDetails.getMpacks().addAll(ambariStackDetails.getMpacks().stream().map(
                        rmpack -> conversionService.convert(rmpack, ManagementPackComponent.class)).collect(Collectors.toList()));
                stackRepoDetailsJson = new Json(stackRepoDetails);
            }
        }
        return new ClusterComponent(ComponentType.HDP_REPO_DETAILS, stackRepoDetailsJson, cluster);
    }

    private StackRepoDetails createStackRepoDetails(DefaultStackRepoDetails stackRepoDetails, String osType) {
        StackRepoDetails repo = new StackRepoDetails();
        repo.setHdpVersion(stackRepoDetails.getHdpVersion());
        repo.setStack(stackRepoDetails.getStack());
        repo.setUtil(stackRepoDetails.getUtil());
        repo.setEnableGplRepo(stackRepoDetails.isEnableGplRepo());
        repo.setVerify(stackRepoDetails.isVerify());
        List<ManagementPackComponent> mpacks = stackRepoDetails.getMpacks().get(osType);
        if (mpacks != null) {
            repo.setMpacks(mpacks);
        }
        return repo;
    }

    private void setOsTypeFromImageIfMissing(Cluster cluster, Optional<Component> stackImageComponent,
            AmbariStackDetailsJson ambariStackDetails) {
        if (StringUtils.isBlank(ambariStackDetails.getOs()) && stackImageComponent.isPresent()) {
            try {
                String osType = getOsType(stackImageComponent);
                if (StringUtils.isNotBlank(osType)) {
                    ambariStackDetails.setOs(osType);
                }
            } catch (IOException e) {
                LOGGER.error("Couldn't convert image component for stack: {} {}", cluster.getStack().getId(), cluster.getStack().getName(), e);
            }
        }
    }

    private String getOsType(Optional<Component> stackImageComponent) throws IOException {
        Image image = stackImageComponent.get().getAttributes().get(Image.class);
        return image.getOsType();
    }

    private boolean containsVDFUrl(Json stackRepoDetailsJson) {
        try {
            return stackRepoDetailsJson.get(StackRepoDetails.class).getStack().containsKey(CUSTOM_VDF_REPO_KEY);
        } catch (IOException e) {
            LOGGER.error("JSON parse error.", e);
        }
        return false;
    }

    private StackInfo defaultHDPInfo(Blueprint blueprint, ClusterRequest request, Organization organization) {
        try {
            JsonNode root = getBlueprintJsonNode(blueprint, request, organization);
            if (root != null) {
                String stackVersion = blueprintUtils.getBlueprintStackVersion(root);
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

    private JsonNode getBlueprintJsonNode(Blueprint blueprint, ClusterRequest request, Organization organization) throws IOException {
        JsonNode root;
        if (blueprint != null) {
            root = JsonUtil.readTree(blueprint.getBlueprintText());
        } else {
            // Backward compatibility to V1 cluster API
            if (request.getBlueprintId() != null) {
                root = JsonUtil.readTree(blueprintService.get(request.getBlueprintId()).getBlueprintText());
            } else if (request.getBlueprintName() != null) {
                root = JsonUtil.readTree(blueprintService.getByNameForOrganization(request.getBlueprintName(), organization).getBlueprintText());
            } else {
                root = JsonUtil.readTree(request.getBlueprint().getAmbariBlueprint());
            }
        }
        return root;
    }

    private String getOsType(Long stackId) throws CloudbreakImageNotFoundException {
        Image image = componentConfigProvider.getImage(stackId);
        return image.getOsType();
    }

    private Optional<String> getVDFUrlByOsType(String osType, DefaultStackRepoDetails stackRepoDetails) {
        String vdfStackRepoKeyFilter = VDF_REPO_KEY_PREFIX;
        if (!StringUtils.isEmpty(osType)) {
            vdfStackRepoKeyFilter += osType;
        }
        String filter = vdfStackRepoKeyFilter;
        return stackRepoDetails.getStack().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(filter))
                .map(Entry::getValue)
                .findFirst();
    }
}
