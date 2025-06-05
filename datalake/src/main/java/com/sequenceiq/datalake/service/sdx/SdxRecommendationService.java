package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_IMAGE_VALIDATION_WARNING;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.requests.ImageRecommendationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageRecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.converter.VmTypeConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDefaultTemplateResponse;
import com.sequenceiq.sdx.api.model.SdxRecommendationResponse;
import com.sequenceiq.sdx.api.model.VmTypeMetaJson;
import com.sequenceiq.sdx.api.model.VmTypeResponse;

@Service
public class SdxRecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRecommendationService.class);

    private static final int IMAGE_VALIDATION_TIMEOUT = 30;

    @Inject
    private CDPConfigService cdpConfigService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private VmTypeConverter vmTypeConverter;

    @Inject
    private ImageCatalogV4Endpoint imageCatalogV4Endpoint;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private RetryService retryService;

    @Inject
    private EntitlementService entitlementService;

    public SdxDefaultTemplateResponse getDefaultTemplateResponse(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform,
            Architecture architecture) {
        StackV4Request defaultTemplate = getDefaultTemplate(clusterShape, runtimeVersion, cloudPlatform, architecture);
        return new SdxDefaultTemplateResponse(defaultTemplate);
    }

    public StackV4Request getDefaultTemplate(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform, Architecture architecture) {
        if (clusterShape == null || StringUtils.isAnyBlank(runtimeVersion, cloudPlatform)) {
            throw new BadRequestException("The following query params needs to be filled for this request: clusterShape, runtimeVersion, cloudPlatform");
        }
        StackV4Request defaultTemplate = cdpConfigService.getConfigForKey(
                new CDPConfigKey(CloudPlatform.valueOf(cloudPlatform), clusterShape, runtimeVersion, architecture));
        if (defaultTemplate == null) {
            LOGGER.warn("Can't find template for cloudplatform: {}, shape {}, cdp version: {}", cloudPlatform, clusterShape, runtimeVersion);
            throw notFound("Default template", "cloudPlatform: " + cloudPlatform + ", shape: " + clusterShape +
                    ", runtime version: " + runtimeVersion).get();
        }
        return defaultTemplate;
    }

    public SdxRecommendationResponse getRecommendation(String credentialCrn, SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform,
            String region, String availabilityZone, String architecture) {
        try {
            availabilityZone = getAvailabilityZoneBasedOnProvider(cloudPlatform, availabilityZone);
            StackV4Request defaultTemplate = getDefaultTemplate(clusterShape, runtimeVersion, cloudPlatform,
                    Optional.ofNullable(architecture).map(Architecture::fromStringWithValidation).orElse(Architecture.X86_64));
            List<VmTypeResponse> availableVmTypes = getAvailableVmTypes(credentialCrn, cloudPlatform, region, availabilityZone, architecture);
            Map<String, VmTypeResponse> defaultVmTypesByInstanceGroup = getDefaultVmTypesByInstanceGroup(availableVmTypes, defaultTemplate);
            Map<String, List<VmTypeResponse>> availableVmTypesByInstanceGroup = filterAvailableVmTypesBasedOnDefault(
                    availableVmTypes, defaultVmTypesByInstanceGroup);
            LOGGER.debug("Return default template and available vm types for clusterShape: {}, " +
                            "runtimeVersion: {}, cloudPlatform: {}, region: {}, availabilityZone: {}",
                    clusterShape, runtimeVersion, cloudPlatform, region, availabilityZone);
            return new SdxRecommendationResponse(defaultTemplate, availableVmTypesByInstanceGroup);
        } catch (NotFoundException | BadRequestException | jakarta.ws.rs.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            if (e.getMessage().contains("The provided client secret keys for app")) {
                throw new BadRequestException(e.getMessage() + " Please update your CDP Credential with the newly generated application key value!");
            }
            LOGGER.warn("Getting recommendation failed!", e);
            throw new RuntimeException("Getting recommendation failed: " + e.getMessage());
        }
    }

    private String getAvailabilityZoneBasedOnProvider(String cloudPlatform, String availabilityZone) {
        if (cloudPlatform.equalsIgnoreCase(CloudPlatform.AZURE.name())) {
            LOGGER.warn("We don't support recommendation by zones on Azure!");
            availabilityZone = null;
        }
        return availabilityZone;
    }

    public void validateVmTypeOverride(DetailedEnvironmentResponse environment, SdxCluster sdxCluster) {
        try {
            LOGGER.debug("Validate vm type override for sdx cluster: {}", sdxCluster.getCrn());
            String cloudPlatform = environment.getCloudPlatform();
            if (shouldValidateVmTypes(sdxCluster, cloudPlatform)) {
                StackV4Request stackV4Request = JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class);
                StackV4Request defaultTemplate = getDefaultTemplate(sdxCluster.getClusterShape(), sdxCluster.getRuntime(), cloudPlatform,
                        sdxCluster.getArchitecture());
                String region = environment.getRegions().getNames().stream().findFirst().orElse(null);
                List<VmTypeResponse> availableVmTypes = getAvailableVmTypes(environment.getCredential().getCrn(), cloudPlatform, region, null,
                        Optional.ofNullable(sdxCluster.getArchitecture()).map(Architecture::getName).orElse(Architecture.X86_64.getName()));
                Map<String, VmTypeResponse> defaultVmTypesByInstanceGroup = getDefaultVmTypesByInstanceGroup(availableVmTypes, defaultTemplate);
                Map<String, List<String>> availableVmTypeNamesByInstanceGroup = filterAvailableVmTypeNamesBasedOnDefault(availableVmTypes,
                        defaultVmTypesByInstanceGroup);

                stackV4Request.getInstanceGroups().forEach(instanceGroup -> {
                    if (!defaultVmTypesByInstanceGroup.containsKey(instanceGroup.getName())) {
                        String message = "Instance group is missing from default template: " + instanceGroup.getName();
                        LOGGER.warn(message);
                        throw new BadRequestException(message);
                    }
                    if (sdxCluster.getArchitecture() != null) {
                        validateInstanceTypeArchitecture(sdxCluster, instanceGroup, availableVmTypes);
                    }
                    VmTypeResponse defaultTemplateVmType = defaultVmTypesByInstanceGroup.get(instanceGroup.getName());
                    if (isCustomInstanceTypeProvided(instanceGroup, defaultTemplateVmType.getValue())
                            && !isProvidedInstanceTypeIsAvailable(availableVmTypeNamesByInstanceGroup, instanceGroup)) {
                        String message = String.format("Invalid custom instance type for instance group: %s - %s",
                                instanceGroup.getName(), instanceGroup.getTemplate().getInstanceType());
                        LOGGER.warn(message);
                        throw new BadRequestException(message);
                    }
                });
            }
        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            if (e.getMessage().contains("The provided client secret keys for app")) {
                throw new BadRequestException(e.getMessage() + " Please update your CDP Credential with the newly generated application key value!");
            }
            LOGGER.warn("Validate VM type override failed!", e);
            throw new RuntimeException("Validate VM type override failed: " + e.getMessage());
        }
    }

    private void validateInstanceTypeArchitecture(SdxCluster sdxCluster, InstanceGroupV4Request instanceGroup, List<VmTypeResponse> availableVmTypes) {
        Optional<String> instanceType = Optional.ofNullable(instanceGroup.getTemplate()).map(InstanceTemplateV4Base::getInstanceType);
        instanceType.ifPresent(s -> availableVmTypes.stream()
                .filter(vmt -> s.equals(vmt.getValue()))
                .findFirst()
                .ifPresent(vmt -> {
                    String architecture = (String) vmt.getVmTypeMetaJson().getProperties().get("Architecture");
                    if (architecture != null && !sdxCluster.getArchitecture().getName().equalsIgnoreCase(architecture)) {
                        String message =
                                String.format("%s instance type has %s cpu architecture which doesn't match the cluster architecture %s",
                                        vmt.getValue(), architecture, sdxCluster.getArchitecture());
                        throw new BadRequestException(message);
                    }
                }));
    }

    private boolean shouldValidateVmTypes(SdxCluster sdxCluster, String cloudPlatform) {
        return !StringUtils.isBlank(sdxCluster.getStackRequest())
                && !SdxClusterShape.CUSTOM.equals(sdxCluster.getClusterShape())
                && !CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform);
    }

    private boolean isProvidedInstanceTypeIsAvailable(Map<String, List<String>> availableVmTypesByInstanceGroup, InstanceGroupV4Request instanceGroup) {
        return availableVmTypesByInstanceGroup.containsKey(instanceGroup.getName())
                && availableVmTypesByInstanceGroup.get(instanceGroup.getName()).contains(instanceGroup.getTemplate().getInstanceType());
    }

    private boolean isCustomInstanceTypeProvided(InstanceGroupV4Request instanceGroup, String defaultTemplateVmType) {
        return !defaultTemplateVmType.equals(instanceGroup.getTemplate().getInstanceType());
    }

    private List<VmTypeResponse> getAvailableVmTypes(String credentialCrn, String cloudPlatform, String region, String availabilityZone, String architecture) {
        PlatformVmtypesResponse platformVmtypesResponse = environmentClientService.getVmTypesByCredential(credentialCrn, region, cloudPlatform,
                CdpResourceType.DATALAKE, availabilityZone, architecture);

        Set<com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse> vmTypes = Collections.emptySet();
        if (platformVmtypesResponse.getVmTypes() != null && StringUtils.isNotBlank(availabilityZone)) {
            vmTypes = platformVmtypesResponse.getVmTypes().get(availabilityZone).getVirtualMachines();
        } else if (platformVmtypesResponse.getVmTypes() != null && !platformVmtypesResponse.getVmTypes().isEmpty()) {
            vmTypes = platformVmtypesResponse.getVmTypes().values().iterator().next().getVirtualMachines();
        }
        return vmTypeConverter.convert(vmTypes);
    }

    private Map<String, List<VmTypeResponse>> filterAvailableVmTypesBasedOnDefault(List<VmTypeResponse> availableVmTypes,
            Map<String, VmTypeResponse> defaultVmTypes) {
        Map<String, List<VmTypeResponse>> filteredVmTypesByInstanceGroup = new HashMap<>();
        for (Entry<String, VmTypeResponse> defaultVmTypeByInstanceGroup : defaultVmTypes.entrySet()) {
            List<VmTypeResponse> filteredVmTypes = filterVmTypes(defaultVmTypeByInstanceGroup.getValue(), availableVmTypes);
            filteredVmTypesByInstanceGroup.put(defaultVmTypeByInstanceGroup.getKey(), filteredVmTypes);
        }

        return filteredVmTypesByInstanceGroup;
    }

    private Map<String, List<String>> filterAvailableVmTypeNamesBasedOnDefault(List<VmTypeResponse> availableVmTypes,
            Map<String, VmTypeResponse> defaultVmTypes) {
        Map<String, List<String>> filteredVmTypesByInstanceGroup = new HashMap<>();
        for (Entry<String, VmTypeResponse> defaultVmTypeByInstanceGroup : defaultVmTypes.entrySet()) {
            List<String> filteredVmTypeNames = filterVmTypes(defaultVmTypeByInstanceGroup.getValue(), availableVmTypes)
                    .stream().map(VmTypeResponse::getValue).collect(Collectors.toList());
            filteredVmTypesByInstanceGroup.put(defaultVmTypeByInstanceGroup.getKey(), filteredVmTypeNames);
        }

        return filteredVmTypesByInstanceGroup;
    }

    private Map<String, VmTypeResponse> getDefaultVmTypesByInstanceGroup(List<VmTypeResponse> availableVmTypes, StackV4Request defaultTemplate) {
        Map<String, VmTypeResponse> vmTypesByName = availableVmTypes.stream().collect(Collectors.toMap(VmTypeResponse::getValue, Function.identity()));
        Map<String, String> defaultInstanceTypesByInstanceGroup = defaultTemplate.getInstanceGroups().stream()
                .filter(instanceGroup ->
                        ObjectUtils.allNotNull(instanceGroup.getName(), instanceGroup.getTemplate(), instanceGroup.getTemplate().getInstanceType()))
                .collect(Collectors.toMap(InstanceGroupV4Request::getName, instanceGroup -> instanceGroup.getTemplate().getInstanceType()));

        Map<String, VmTypeResponse> defaultVmTypesByInstanceGroup = new HashMap<>();
        for (Entry<String, String> instanceGroup : defaultInstanceTypesByInstanceGroup.entrySet()) {
            String instanceGroupName = instanceGroup.getKey();
            String instanceType = instanceGroup.getValue();
            if (!vmTypesByName.containsKey(instanceType)) {
                String message = String.format("Missing vm type for default template instance group: %s - %s", instanceGroupName, instanceType);
                LOGGER.warn(message);
                throw new BadRequestException(message);
            }
            defaultVmTypesByInstanceGroup.put(instanceGroupName, vmTypesByName.get(instanceType));
        }
        return defaultVmTypesByInstanceGroup;
    }

    private List<VmTypeResponse> filterVmTypes(VmTypeResponse defaultVmType, List<VmTypeResponse> availableVmTypes) {
        return availableVmTypes.stream().filter(vmType -> filterVmTypeLargerThanDefault(defaultVmType, vmType)).collect(Collectors.toList());
    }

    private boolean filterVmTypeLargerThanDefault(VmTypeResponse defaultVmType, VmTypeResponse vmType) {
        if (!ObjectUtils.allNotNull(defaultVmType, defaultVmType.getVmTypeMetaJson(), vmType, vmType.getVmTypeMetaJson())) {
            return false;
        }
        VmTypeMetaJson defaultVmTypeMetaData = defaultVmType.getVmTypeMetaJson();
        VmTypeMetaJson vmTypeMetaData = vmType.getVmTypeMetaJson();
        if (!ObjectUtils.allNotNull(defaultVmTypeMetaData.getCPU(), defaultVmTypeMetaData.getMemoryInGb(),
                vmTypeMetaData.getCPU(), vmTypeMetaData.getMemoryInGb())) {
            return false;
        }
        return vmTypeMetaData.getCPU() >= defaultVmTypeMetaData.getCPU() && vmTypeMetaData.getMemoryInGb() >= defaultVmTypeMetaData.getMemoryInGb();
    }

    public void validateRecommendedImage(DetailedEnvironmentResponse environmentResponse, SdxCluster sdxCluster) {
        boolean azureMarketplaceImagesEnabled = isAzureMarketplaceImagesEnabled();
        if (!(CloudPlatform.AZURE.equalsIgnoreCase(environmentResponse.getCloudPlatform()) && azureMarketplaceImagesEnabled)) {
            LOGGER.debug("Skipping image validation on platforms other than Azure or Azure marketplace image entitlement is not enabled. " +
                    "Platform: [{}], AzureMarketplaceImagesEnabled entitlement: [{}]", environmentResponse.getCloudPlatform(), azureMarketplaceImagesEnabled);
            return;
        }
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ImageRecommendationV4Response response =
                retryService.testWith1SecDelayMax5Times(() -> executeImageValidationWithTimeout(environmentResponse, sdxCluster, executor));
        if (response.hasValidationError()) {
            LOGGER.debug("Validation finished with an error: {}", response.hasValidationError());
            throw new BadRequestException(response.getValidationMessage());
        } else if (StringUtils.isNotEmpty(response.getValidationMessage())) {
            LOGGER.debug("Validation finished with a warning: {}", response.getValidationMessage());
            eventSenderService.sendEventAndNotification(sdxCluster, DATALAKE_IMAGE_VALIDATION_WARNING, List.of(response.getValidationMessage()));
        }
    }

    private boolean isAzureMarketplaceImagesEnabled() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return entitlementService.azureMarketplaceImagesEnabled(accountId);
    }

    private ImageRecommendationV4Response executeImageValidationWithTimeout(DetailedEnvironmentResponse environmentResponse, SdxCluster sdxCluster,
            ExecutorService executor) {
        Future<ImageRecommendationV4Response> responseFuture = executor.submit(() -> doImageValidation(environmentResponse, sdxCluster));
        try {
            return responseFuture.get(IMAGE_VALIDATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.debug("Image validation request timeout [{}s] happened. Cancelling request thread", IMAGE_VALIDATION_TIMEOUT);
            responseFuture.cancel(true);
        } catch (ExecutionException e) {
            LOGGER.warn("An error occurred while executing the image validation request.", e);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread was interruped during image validation", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
        return new ImageRecommendationV4Response();
    }

    private ImageRecommendationV4Response doImageValidation(DetailedEnvironmentResponse environmentResponse, SdxCluster sdxCluster) {
        try {
            ImageRecommendationV4Request request = createImageValidationRequest(environmentResponse, sdxCluster);
            LOGGER.debug("Calling recommended image validation with: {}", request);
            return imageCatalogV4Endpoint.validateRecommendedImageWithProvider(SdxService.WORKSPACE_ID_DEFAULT, request);
        } catch (Exception e) {
            LOGGER.warn("Failed to validate recommended image for Data Lake.", e);
            return new ImageRecommendationV4Response();
        }
    }

    @NotNull
    private ImageRecommendationV4Request createImageValidationRequest(DetailedEnvironmentResponse environmentResponse, SdxCluster sdxCluster)
            throws IOException {
        StackV4Request stackV4Request = JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class);
        String cloudPlatform = environmentResponse.getCloudPlatform();
        String region = environmentResponse.getRegions().getNames().stream().findFirst().orElse(null);
        StackV4Request defaultTemplate = getDefaultTemplate(sdxCluster.getClusterShape(), sdxCluster.getRuntime(), cloudPlatform, sdxCluster.getArchitecture());
        ImageRecommendationV4Request request = new ImageRecommendationV4Request();
        request.setBlueprintName(defaultTemplate.getCluster().getBlueprintName());
        request.setPlatform(cloudPlatform);
        request.setEnvironmentCrn(sdxCluster.getEnvCrn());
        request.setRegion(region);
        request.setImage(stackV4Request.getImage());
        return request;
    }
}
