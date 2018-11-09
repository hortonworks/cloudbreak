package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.CUSTOM_VDF_REPO_KEY;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.VDF_REPO_KEY_PREFIX;
import static com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_6_0_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.api.model.stack.StackMatrix;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
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
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.ClusterCreationEnvironmentValidator;
import com.sequenceiq.cloudbreak.controller.validation.filesystem.FileSystemValidator;
import com.sequenceiq.cloudbreak.controller.validation.mpack.ManagementPackValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConfigValidator;
import com.sequenceiq.cloudbreak.converter.AmbariStackDetailsJsonToStackRepoDetailsConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
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

    private static final Pattern MAJOR_VERSION_REGEX_PATTERN = Pattern.compile("(^[0-9]+\\.[0-9]+).*");

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
    private StackMatrixService stackMatrixService;

    @Inject
    private ManagementPackValidator mpackValidator;

    @Inject
    private RdsConfigValidator rdsConfigValidator;

    @Inject
    private ClusterCreationEnvironmentValidator environmentValidator;

    public void validate(ClusterRequest request, Stack stack, User user, Workspace workspace) {
        validate(request, null, stack, user, workspace);
    }

    public void validate(ClusterRequest request, CloudCredential cloudCredential, Stack stack, User user, Workspace workspace) {
        if (request.getEnableSecurity() && request.getKerberos() == null) {
            throw new BadRequestException("If the security is enabled the kerberos parameters cannot be empty");
        }
        MDCBuilder.buildUserMdcContext(user.getUserId());
        CloudCredential credential = cloudCredential;
        if (credential == null) {
            credential = credentialToCloudCredentialConverter.convert(stack.getCredential());
        }
        fileSystemValidator.validateFileSystem(stack.cloudPlatform(), credential, request.getFileSystem(),
                stack.getCreator().getUserId(), stack.getWorkspace().getId());
        mpackValidator.validateMpacks(request, workspace);
        rdsConfigValidator.validateRdsConfigs(request, user, workspace);
        ValidationResult environmentValidationResult = environmentValidator.validate(request, stack);
        if (environmentValidationResult.hasError()) {
            throw new BadRequestException(environmentValidationResult.getFormattedErrors());
        }
    }

    public Cluster prepare(ClusterRequest request, Stack stack, User user, Workspace workspace) throws CloudbreakImageNotFoundException,
            IOException, TransactionExecutionException {
        return prepare(request, stack, null, user, workspace);
    }

    public Cluster prepare(ClusterRequest request, Stack stack, Blueprint blueprint, User user, Workspace workspace) throws IOException,
            CloudbreakImageNotFoundException, TransactionExecutionException {
        String stackName = stack.getName();

        long start = System.currentTimeMillis();

        if (request.getFileSystem() != null) {
            FileSystem fs = fileSystemConfigService.create(conversionService.convert(request.getFileSystem(), FileSystem.class), stack.getWorkspace(),
                    stack.getCreator());
            request.getFileSystem().setName(fs.getName());
            LOGGER.info("File system saving took {} ms for stack {}", System.currentTimeMillis() - start, stackName);
        }

        Cluster cluster = conversionService.convert(request, Cluster.class);
        cluster.setStack(stack);
        cluster.setWorkspace(stack.getWorkspace());
        LOGGER.info("Cluster conversion took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        start = System.currentTimeMillis();

        cluster = clusterDecorator.decorate(cluster, request, blueprint, user, stack.getWorkspace(), stack);
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
        ClusterComponent hdpRepoConfig = determineHDPRepoConfig(blueprint, stack.getId(), stackHdpRepoConfig, request, cluster, stack.getWorkspace(),
                stackImageComponent);
        components.add(hdpRepoConfig);

        checkRepositories(ambariRepoConfig, hdpRepoConfig, stackImageComponent.get(), request.getValidateRepositories());
        checkVDFFile(ambariRepoConfig, hdpRepoConfig, stackName);

        LOGGER.info("Cluster components saved in {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        start = System.currentTimeMillis();
        Cluster savedCluster = clusterService.create(stack, cluster, components, user);
        LOGGER.info("Cluster object creation took {} ms for stack {}", System.currentTimeMillis() - start, stackName);

        return savedCluster;
    }

    private void checkRepositories(ClusterComponent ambariRepoComponent, ClusterComponent stackRepoComponent, Component imageComponent, boolean strictCheck)
            throws IOException {
        AmbariRepo ambariRepo = ambariRepoComponent.getAttributes().get(AmbariRepo.class);
        StackRepoDetails stackRepoDetails = stackRepoComponent.getAttributes().get(StackRepoDetails.class);
        Image image = imageComponent.getAttributes().get(Image.class);
        StackMatrix stackMatrix = stackMatrixService.getStackMatrix();
        String stackMajorVersion = stackRepoDetails.getHdpVersion();
        Matcher majorVersionRegex = MAJOR_VERSION_REGEX_PATTERN.matcher(stackMajorVersion);
        if (majorVersionRegex.matches()) {
            stackMajorVersion = majorVersionRegex.group(1);
        }
        Map<String, StackDescriptor> stackDescriptorMap;
        String stackType = stackRepoDetails.getStack().get(StackRepoDetails.REPO_ID_TAG);
        if (stackType.contains("-")) {
            stackType = stackType.substring(0, stackType.indexOf("-"));
        }
        switch (stackType) {
            case "HDP":
                stackDescriptorMap = stackMatrix.getHdp();
                break;
            case "HDF":
                stackDescriptorMap = stackMatrix.getHdf();
                break;
            default:
                LOGGER.warn("No stack descriptor map found for stacktype {}, using 'HDP'", stackType);
                stackDescriptorMap = stackMatrix.getHdp();
        }
        StackDescriptor stackDescriptor = stackDescriptorMap.get(stackMajorVersion);
        if (stackDescriptor != null) {
            boolean hasDefaultStackRepoUrlForOsType = stackDescriptor.getRepo().getStack().containsKey(image.getOsType());
            boolean hasDefaultAmbariRepoUrlForOsType = stackDescriptor.getAmbari().getRepo().containsKey(image.getOsType());
            boolean compatibleAmbari = new VersionComparator().compare(() -> ambariRepo.getVersion().substring(0, stackDescriptor.getMinAmbari().length()),
                    () -> stackDescriptor.getMinAmbari()) >= 0;
            if (!hasDefaultAmbariRepoUrlForOsType || !hasDefaultStackRepoUrlForOsType || !compatibleAmbari) {
                String message = String.format("The given repository information seems to be incompatible."
                                + " Ambari version: %s, Stack type: %s, Stack version: %s, Image Id: %s, Os type: %s.", ambariRepo.getVersion(),
                        stackType, stackRepoDetails.getHdpVersion(), image.getImageId(), image.getOsType());
                if (strictCheck) {
                    LOGGER.error(message);
                    throw new BadRequestException(message);
                } else {
                    LOGGER.warn(message);
                }
            }
        }
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
            String blueprintText = cluster.getBlueprint().getBlueprintText().getRaw();
            JsonNode bluePrintJson = JsonUtil.readTree(blueprintText);
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
            ClusterRequest request, Cluster cluster, Workspace workspace, Optional<Component> stackImageComponent)
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
                DefaultStackRepoDetails stackRepoDetails = SerializationUtils.clone(defaultHDPInfo(blueprint, request, workspace).getRepo());
                String osType = getOsType(stackId);
                StackRepoDetails repo = createStackRepoDetails(stackRepoDetails, osType);
                Optional<String> vdfUrl = getVDFUrlByOsType(osType, stackRepoDetails);
                vdfUrl.ifPresent(s -> stackRepoDetails.getStack().put(CUSTOM_VDF_REPO_KEY, s));
                if (ambariStackDetails != null) {
                    repo.getMpacks().addAll(ambariStackDetails.getMpacks().stream().map(
                            rmpack -> conversionService.convert(rmpack, ManagementPackComponent.class)).collect(Collectors.toList()));
                }
                setEnableGplIfAvailable(repo, ambariStackDetails);
                stackRepoDetailsJson = new Json(repo);
            }
        } else {
            stackRepoDetailsJson = stackHdpRepoConfig.get().getAttributes();
            StackRepoDetails stackRepoDetails = stackRepoDetailsJson.get(StackRepoDetails.class);
            if (ambariStackDetails != null && !ambariStackDetails.getMpacks().isEmpty()) {
                stackRepoDetails.getMpacks().addAll(ambariStackDetails.getMpacks().stream().map(
                        rmpack -> conversionService.convert(rmpack, ManagementPackComponent.class)).collect(Collectors.toList()));
            }
            setEnableGplIfAvailable(stackRepoDetails, ambariStackDetails);
            stackRepoDetailsJson = new Json(stackRepoDetails);
        }
        return new ClusterComponent(ComponentType.HDP_REPO_DETAILS, stackRepoDetailsJson, cluster);
    }

    private void setEnableGplIfAvailable(StackRepoDetails repo, AmbariStackDetailsJson ambariStackDetails) {
        if (ambariStackDetails != null) {
            repo.setEnableGplRepo(ambariStackDetails.isEnableGplRepo());
        }
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

    private StackInfo defaultHDPInfo(Blueprint blueprint, ClusterRequest request, Workspace workspace) {
        try {
            JsonNode root = getBlueprintJsonNode(blueprint, request, workspace);
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

    private JsonNode getBlueprintJsonNode(Blueprint blueprint, ClusterRequest request, Workspace workspace) throws IOException {
        JsonNode root;
        if (blueprint != null) {
            String blueprintText = blueprint.getBlueprintText().getRaw();
            root = JsonUtil.readTree(blueprintText);
        } else {
            // Backward compatibility to V1 cluster API
            if (request.getBlueprintId() != null) {
                root = JsonUtil.readTree(blueprintService.get(request.getBlueprintId()).getBlueprintText().getRaw());
            } else if (request.getBlueprintName() != null) {
                blueprint = blueprintService.getByNameForWorkspace(request.getBlueprintName(), workspace);
                String blueprintText = blueprint.getBlueprintText().getRaw();
                root = JsonUtil.readTree(blueprintText);
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
