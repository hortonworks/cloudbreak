package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.CUSTOM_VDF_REPO_KEY;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.VDF_REPO_KEY_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.AmbariStackDescriptorV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.AmbariDefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDFInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPEntries;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultHDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.api.ClusterPreCreationApi;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari.StackRepositoryV4RequestToStackRepoDetailsConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class AmbariClusterCreationSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterCreationSetupService.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private DefaultAmbariRepoService defaultAmbariRepoService;

    @Inject
    private DefaultHDPEntries defaultHDPEntries;

    @Inject
    private DefaultHDFEntries defaultHDFEntries;

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private StackRepositoryV4RequestToStackRepoDetailsConverter stackRepoDetailsConverter;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private BlueprintUtils blueprintUtils;

    public List<ClusterComponent> prepareAmbariCluster(ClusterV4Request request, Stack stack, Blueprint blueprint, Cluster cluster,
            Optional<Component> stackAmbariRepoConfig, Optional<Component> stackHdpRepoConfig, Optional<Component> stackImageComponent) throws IOException,
            CloudbreakImageNotFoundException {
        List<ClusterComponent> components = new ArrayList<>();
        AmbariRepositoryV4Request repoDetailsJson = Optional.ofNullable(request.getAmbari()).map(AmbariV4Request::getRepository).orElse(null);
        ClusterComponent ambariRepoConfig = determineAmbariRepoConfig(stackAmbariRepoConfig, repoDetailsJson, stackImageComponent, cluster);
        components.add(ambariRepoConfig);
        ClusterComponent hdpRepoConfig = determineHDPRepoConfig(blueprint,
                stack.getId(), stackHdpRepoConfig, request, cluster, stack.getWorkspace(),
                stackImageComponent);
        components.add(hdpRepoConfig);
        checkAmbariStackRepositories(ambariRepoConfig, hdpRepoConfig, stackImageComponent.get());
        checkVDFFile(ambariRepoConfig, hdpRepoConfig, cluster);
        return components;
    }

    private void checkAmbariStackRepositories(ClusterComponent ambariRepoComponent, ClusterComponent stackRepoComponent, Component imageComponent)
            throws IOException {
        AmbariRepo ambariRepo = ambariRepoComponent.getAttributes().get(AmbariRepo.class);
        StackRepoDetails stackRepoDetails = stackRepoComponent.getAttributes().get(StackRepoDetails.class);
        Image image = imageComponent.getAttributes().get(Image.class);
        StackMatrixV4Response stackMatrixV4Response = stackMatrixService.getStackMatrix();
        String stackMajorVersion = stackRepoDetails.getMajorHdpVersion();
        Map<String, AmbariStackDescriptorV4Response> stackDescriptorMap;

        String stackType = stackRepoDetails.getStack().get(StackRepoDetails.REPO_ID_TAG);
        if (stackType.contains("-")) {
            stackType = stackType.substring(0, stackType.indexOf('-'));
        }
        switch (stackType) {
            case "HDP":
                stackDescriptorMap = stackMatrixV4Response.getHdp();
                break;
            case "HDF":
                stackDescriptorMap = stackMatrixV4Response.getHdf();
                break;
            default:
                LOGGER.warn("No stack descriptor map found for stacktype {}, using 'HDP'", stackType);
                stackDescriptorMap = stackMatrixV4Response.getHdp();
        }
        AmbariStackDescriptorV4Response stackDescriptorV4 = stackDescriptorMap.get(stackMajorVersion);
        if (stackDescriptorV4 != null) {
            boolean hasDefaultStackRepoUrlForOsType = stackDescriptorV4.getRepository().getStack().containsKey(image.getOsType());
            boolean hasDefaultAmbariRepoUrlForOsType = stackDescriptorV4.getAmbari().getRepository().containsKey(image.getOsType());
            boolean compatibleAmbari = new VersionComparator().compare(() -> ambariRepo.getVersion().substring(0, stackDescriptorV4.getMinAmbari().length()),
                    stackDescriptorV4::getMinAmbari) >= 0;
            if (!hasDefaultAmbariRepoUrlForOsType || !hasDefaultStackRepoUrlForOsType || !compatibleAmbari) {
                String message = String.format("The given repository information seems to be incompatible."
                                + " Ambari version: %s, Stack type: %s, Stack version: %s, Image Id: %s, Os type: %s.", ambariRepo.getVersion(),
                        stackType, stackRepoDetails.getHdpVersion(), image.getImageId(), image.getOsType());
                LOGGER.warn(message);
            }
        }
    }

    public void checkVDFFile(ClusterComponent ambariRepoConfig, ClusterComponent hdpRepoConfig, Cluster cluster) throws IOException {
        AmbariRepo ambariRepo = ambariRepoConfig.getAttributes().get(AmbariRepo.class);
        ClusterPreCreationApi connector = clusterApiConnectors.getConnector(cluster);
        if (connector.isVdfReady(ambariRepo) && !containsVDFUrl(hdpRepoConfig.getAttributes())) {
            throw new BadRequestException(String.format("Couldn't determine any VDF file for the stack: %s", cluster.getName()));
        }
    }

    private ClusterComponent determineAmbariRepoConfig(Optional<Component> stackAmbariRepoConfig,
            AmbariRepositoryV4Request ambariRepoDetailsJson, Optional<Component> stackImageComponent, Cluster cluster) throws IOException {
        Json json;
        if (Objects.isNull(stackAmbariRepoConfig) || !stackAmbariRepoConfig.isPresent()) {
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            JsonNode bluePrintJson = JsonUtil.readTree(blueprintText);
            String stackVersion = blueprintUtils.getBlueprintStackVersion(bluePrintJson);
            String stackName = blueprintUtils.getBlueprintStackName(bluePrintJson);
            AmbariRepo ambariRepo = ambariRepoDetailsJson != null
                    ? converterUtil.convert(ambariRepoDetailsJson, AmbariRepo.class)
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
            ClusterV4Request request, Cluster cluster, Workspace workspace, Optional<Component> stackImageComponent)
            throws IOException, CloudbreakImageNotFoundException {
        Json stackRepoDetailsJson;
        StackRepositoryV4Request ambariStackDetails = request.getAmbari() == null ? null : request.getAmbari().getStackRepository();
        if (Objects.isNull(stackHdpRepoConfig) || !stackHdpRepoConfig.isPresent()) {
            stackRepoDetailsJson = determineStackRepoDetails(blueprint, stackId, request, cluster, workspace, stackImageComponent, ambariStackDetails);
        } else {
            stackRepoDetailsJson = stackHdpRepoConfig.get().getAttributes();
            StackRepoDetails stackRepoDetails = stackRepoDetailsJson.get(StackRepoDetails.class);
            if (ambariStackDetails != null && !ambariStackDetails.getMpacks().isEmpty()) {
                stackRepoDetails.getMpacks().addAll(ambariStackDetails.getMpacks().stream().map(
                        rmpack -> converterUtil.convert(rmpack, ManagementPackComponent.class)).collect(Collectors.toList()));
            }
            setEnableGplIfAvailable(stackRepoDetails, ambariStackDetails);
            stackRepoDetailsJson = new Json(stackRepoDetails);
        }
        return new ClusterComponent(ComponentType.HDP_REPO_DETAILS, stackRepoDetailsJson, cluster);
    }

    private Json determineStackRepoDetails(Blueprint blueprint, long stackId, ClusterV4Request request,
            Cluster cluster, Workspace workspace, Optional<Component> stackImageComponent, StackRepositoryV4Request
            ambariStackDetails) throws JsonProcessingException, CloudbreakImageNotFoundException {
        Json stackRepoDetailsJson;
        if (ambariStackDetails != null && (stackRepoDetailsConverter.isBaseRepoRequiredFieldsExists(ambariStackDetails)
                || stackRepoDetailsConverter.isVdfRequiredFieldsExists(ambariStackDetails))) {
            setOsTypeFromImageIfMissing(cluster, stackImageComponent, ambariStackDetails);
            StackRepoDetails stackRepoDetails = stackRepoDetailsConverter.convert(ambariStackDetails);
            stackRepoDetailsJson = new Json(stackRepoDetails);
        } else {
            AmbariDefaultStackRepoDetails stackRepoDetails = SerializationUtils.clone(defaultHDPInfo(blueprint, request, workspace).getRepo());
            String osType = getOsType(stackId);
            StackRepoDetails repo = createStackRepoDetails(stackRepoDetails, osType);
            Optional<String> vdfUrl = getVDFUrlByOsType(osType, stackRepoDetails);
            vdfUrl.ifPresent(s -> stackRepoDetails.getStack().put(StackRepoDetails.CUSTOM_VDF_REPO_KEY, s));
            if (ambariStackDetails != null && ambariStackDetails.getMpacks() != null) {
                repo.getMpacks().addAll(ambariStackDetails.getMpacks().stream().map(
                        rmpack -> converterUtil.convert(rmpack, ManagementPackComponent.class)).collect(Collectors.toList()));
            }
            setEnableGplIfAvailable(repo, ambariStackDetails);
            stackRepoDetailsJson = new Json(repo);
        }
        return stackRepoDetailsJson;
    }

    private StackInfo defaultHDPInfo(Blueprint blueprint, ClusterV4Request request, Workspace workspace) {
        try {
            JsonNode root = getBlueprintJsonNode(blueprint, request, workspace);
            if (root != null) {
                String stackVersion = blueprintUtils.getBlueprintStackVersion(root);
                String stackName = blueprintUtils.getBlueprintStackName(root);
                if ("HDF".equalsIgnoreCase(stackName)) {
                    LOGGER.debug("Stack name is HDF, use the default HDF repo for version: " + stackVersion);
                    for (Map.Entry<String, DefaultHDFInfo> entry : defaultHDFEntries.getEntries().entrySet()) {
                        if (entry.getKey().equals(stackVersion)) {
                            return entry.getValue();
                        }
                    }
                } else {
                    LOGGER.debug("Stack name is HDP, use the default HDP repo for version: " + stackVersion);
                    for (Map.Entry<String, DefaultHDPInfo> entry : defaultHDPEntries.getEntries().entrySet()) {
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

    private JsonNode getBlueprintJsonNode(Blueprint blueprint, ClusterV4Request request, Workspace workspace) throws IOException {
        JsonNode root = null;
        if (blueprint != null) {
            String blueprintText = blueprint.getBlueprintText();
            root = JsonUtil.readTree(blueprintText);
        } else if (request.getBlueprintName() != null) {
            blueprint = blueprintService.getByNameForWorkspace(request.getBlueprintName(), workspace);
            String blueprintText = blueprint.getBlueprintText();
            root = JsonUtil.readTree(blueprintText);
        }
        return root;
    }

    private boolean containsVDFUrl(Json stackRepoDetailsJson) {
        try {
            return stackRepoDetailsJson.get(StackRepoDetails.class).getStack().containsKey(CUSTOM_VDF_REPO_KEY);
        } catch (IOException e) {
            LOGGER.error("JSON parse error.", e);
        }
        return false;
    }

    private String getOsType(Long stackId) throws CloudbreakImageNotFoundException {
        Image image = componentConfigProviderService.getImage(stackId);
        return image.getOsType();
    }

    private String getOsType(Optional<Component> stackImageComponent) throws IOException {
        Image image = stackImageComponent.get().getAttributes().get(Image.class);
        return image.getOsType();
    }

    private Optional<String> getVDFUrlByOsType(String osType, AmbariDefaultStackRepoDetails stackRepoDetails) {
        String vdfStackRepoKeyFilter = VDF_REPO_KEY_PREFIX;
        if (!StringUtils.isEmpty(osType)) {
            vdfStackRepoKeyFilter += osType;
        }
        String filter = vdfStackRepoKeyFilter;
        return stackRepoDetails.getStack().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(filter))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private StackRepoDetails createStackRepoDetails(AmbariDefaultStackRepoDetails stackRepoDetails, String osType) {
        StackRepoDetails repo = new StackRepoDetails();
        repo.setHdpVersion(stackRepoDetails.getHdpVersion());
        repo.setStack(stackRepoDetails.getStack());
        repo.setUtil(stackRepoDetails.getUtil());
        repo.setEnableGplRepo(stackRepoDetails.isEnableGplRepo());
        repo.setVerify(stackRepoDetails.isVerify());
        List<ManagementPackComponent> mpacks = stackRepoDetails.getMpacks().get(osType);
        repo.setMpacks(Objects.requireNonNullElseGet(mpacks, LinkedList::new));
        return repo;
    }

    private void setOsTypeFromImageIfMissing(Cluster cluster, Optional<Component> stackImageComponent,
            StackRepositoryV4Request ambariStackDetails) {
        if (StringUtils.isBlank(ambariStackDetails.getOs()) && stackImageComponent.isPresent()) {
            try {
                String osType = getOsType(stackImageComponent);
                if (StringUtils.isNotBlank(osType)) {
                    ambariStackDetails.setOs(osType);
                }
            } catch (IOException e) {
                LOGGER.info("Couldn't convert image component for stack: {} {}", cluster.getStack().getId(), cluster.getStack().getName(), e);
            }
        }
    }

    private void setEnableGplIfAvailable(StackRepoDetails repo, StackRepositoryV4Request ambariStackDetails) {
        if (ambariStackDetails != null) {
            repo.setEnableGplRepo(ambariStackDetails.isEnableGplRepo());
        }
    }
}