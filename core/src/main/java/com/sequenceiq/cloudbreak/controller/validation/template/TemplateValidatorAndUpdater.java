package com.sequenceiq.cloudbreak.controller.validation.template;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_DEFAULT_CHANGED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_FILTERED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_ROOT_VOLUME_TYPE_CHANGED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_VOLUME_SIZE_INCREASED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.controller.validation.template.azure.HostEncryptionProvider;
import com.sequenceiq.cloudbreak.controller.validation.template.azure.ResourceDiskPropertyCalculator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class TemplateValidatorAndUpdater {

    public static final String GROUP_NAME_ID_BROKER = "idbroker";

    public static final String ROLE_IMPALAD = "IMPALAD";

    private static final Set<String> SDX_COMPUTE_INSTANCES = Set.of(InstanceGroupName.HMS_SCALE_OUT.getName(), InstanceGroupName.RAZ_SCALE_OUT.getName(),
            InstanceGroupName.ATLAS_SCALE_OUT.getName());

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateValidatorAndUpdater.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private EmptyVolumeSetFilter emptyVolumeSetFilter;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private LocationService locationService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private ResourceDiskPropertyCalculator resourceDiskPropertyCalculator;

    @Inject
    private HostEncryptionProvider hostEncryptionProvider;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Value("${cb.doc.urls.supportedInstanceTypes:https://www.cloudera.com/products/pricing/cdp-public-cloud-service-rates.html}")
    private String supportedVmTypesDocPageLink;

    private final Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters =
            Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    @PostConstruct
    private void postConstructChecks() {
        if (isEmpty(supportedVmTypesDocPageLink)) {
            LOGGER.warn("The documentation link for the supported instance types has not filled hence we're unable to provide it" +
                    " for the customer in the case of an invalid request!");
        }
    }

    public void validate(DetailedEnvironmentResponse environment, Credential credential, InstanceGroup instanceGroup, Stack stack,
            CdpResourceType stackType, String imageHyperVGeneration, ValidationResult.ValidationResultBuilder validationBuilder) {
        Template template = instanceGroup.getTemplate();
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                stack.getRegion(),
                stack.getPlatformVariant(),
                stackType,
                Map.of(ARCHITECTURE, stack.getArchitectureName()));

        if (isEmpty(template.getInstanceType())) {
            validateCustomInstanceType(template, validationBuilder);
        } else {
            VmType vmType = null;
            Platform platform = Platform.platform(template.getCloudPlatform());
            Map<String, Set<VmType>> machines = cloudVmTypes.getCloudVmResponses();
            String locationString = locationService.location(stack.getRegion(), stack.getAvailabilityZone());
            boolean fallbackEnabled = entitlementService.isFallbackInstanceTypeEnabled(credential.getAccount());
            List<String> possibleInstanceTypes = new ArrayList<>();
            possibleInstanceTypes.add(template.getInstanceType());
            if (fallbackEnabled) {
                possibleInstanceTypes.addAll(template.getFallbackInstanceTypesAsList());
            }
            if (machines.containsKey(locationString) && !machines.get(locationString).isEmpty()) {
                InstanceTypeAvailability availability = getInstanceTypeAvailability(environment, instanceGroup, stack, machines, locationString,
                        fallbackEnabled);
                Set<VmType> availableVmTypes = availability.vmTypes();
                String availabilityLocation = availability.location();
                if (fallbackEnabled) {
                    possibleInstanceTypes = filterInstanceTypesByAvailability(possibleInstanceTypes, availableVmTypes, template, instanceGroup,
                            availabilityLocation);
                }
                vmType = selectVmTypeOrNull(possibleInstanceTypes, availableVmTypes, machines.get(locationString));
                if (vmType == null) {
                    LOGGER.info("Instance type not found {} at location {}. Available instances: {}", possibleInstanceTypes, locationString,
                            machines.get(locationString).stream().map(StringType::value).toList());
                    validationBuilder.error(getInvalidVmTypeErrorMessage(possibleInstanceTypes, platform.value(), stack.getRegion()));
                } else {
                    boolean architectureMatching = isArchitectureMatching(vmType, stack, validationBuilder);
                    boolean hyperVGenerationSupported = isHyperVGenerationSupported(vmType, imageHyperVGeneration, validationBuilder);
                    if (architectureMatching && hyperVGenerationSupported) {
                        setRootVolumeTypeIfEmpty(template, vmType, possibleInstanceTypes.subList(1, possibleInstanceTypes.size()), instanceGroup);
                        if (fallbackEnabled) {
                            updateInstanceTypes(possibleInstanceTypes, availableVmTypes, template, instanceGroup, stack, availabilityLocation);
                        }
                    }
                }
            }
            template = emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(instanceGroup.getTemplate());
            template = resourceDiskPropertyCalculator.updateWithResourceDiskAttached(credential, template, vmType);
            template = hostEncryptionProvider.updateWithHostEncryption(environment, credential, template, vmType);
            validateVolumeTemplates(template, vmType, platform, validationBuilder, instanceGroup, stack);
            validateMaximumVolumeSize(template, vmType, validationBuilder);
        }
    }

    private InstanceTypeAvailability getInstanceTypeAvailability(DetailedEnvironmentResponse environment, InstanceGroup instanceGroup, Stack stack,
            Map<String, Set<VmType>> machines, String locationString, boolean zoneAware) {
        Set<String> availabilityZones = zoneAware ? getAvailabilityZonesOfGroup(environment, instanceGroup, stack) : Set.of();
        if (availabilityZones.isEmpty()) {
            return new InstanceTypeAvailability(machines.get(locationString), locationString);
        }
        return new InstanceTypeAvailability(getVmTypesAvailableInAnyZone(machines, machines.get(locationString), availabilityZones),
                String.join(", ", new TreeSet<>(availabilityZones)));
    }

    private Set<String> getAvailabilityZonesOfGroup(DetailedEnvironmentResponse environment, InstanceGroup instanceGroup, Stack stack) {
        Set<String> groupZones = filterBlankZones(instanceGroup.getAvailabilityZones());
        if (!groupZones.isEmpty()) {
            return groupZones;
        }
        if (stack.isMultiAz()) {
            CloudPlatform cloudPlatform = CloudPlatform.fromName(instanceGroup.getTemplate().getCloudPlatform());
            if (cloudPlatform != null && environment != null && environment.getNetwork() != null) {
                return filterBlankZones(environment.getNetwork().getAvailabilityZones(cloudPlatform));
            }
        }
        return Set.of();
    }

    private Set<String> filterBlankZones(Set<String> zones) {
        if (zones == null) {
            return Set.of();
        }
        return zones.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private Set<VmType> getVmTypesAvailableInAnyZone(Map<String, Set<VmType>> machines, Set<VmType> vmTypesOfLocation, Set<String> availabilityZones) {
        Set<VmType> vmTypes = new HashSet<>(vmTypesOfLocation);
        availabilityZones.stream()
                .map(machines::get)
                .filter(Objects::nonNull)
                .forEach(vmTypes::addAll);
        return vmTypes.stream()
                .filter(vmType -> isAvailableInAnyZone(vmType, availabilityZones))
                .collect(Collectors.toSet());
    }

    private boolean isAvailableInAnyZone(VmType vmType, Set<String> availabilityZones) {
        if (!vmType.isMetaSet() || CollectionUtils.isEmpty(vmType.getMetaData().getAvailabilityZones())) {
            return true;
        }
        return vmType.getMetaData().getAvailabilityZones().stream().anyMatch(availabilityZones::contains);
    }

    private List<String> filterInstanceTypesByAvailability(List<String> possibleInstanceTypes, Set<VmType> availableVmTypes, Template template,
            InstanceGroup instanceGroup, String availabilityLocation) {
        Map<String, VmType> availableVmTypesByName = availableVmTypes.stream()
                .collect(Collectors.toMap(StringType::value, Function.identity(), (first, second) -> first));
        List<String> filteredInstanceTypes = possibleInstanceTypes.stream().filter(availableVmTypesByName::containsKey).distinct().toList();
        if (filteredInstanceTypes.isEmpty()) {
            LOGGER.info("None of the requested instance types {} are available at location {} for group {}, leaving the template untouched.",
                    possibleInstanceTypes, availabilityLocation, instanceGroup.getGroupName());
            return possibleInstanceTypes;
        }
        if (!filteredInstanceTypes.getFirst().equals(template.getInstanceType())) {
            return promoteFirstVolumeCompatibleInstanceType(filteredInstanceTypes, availableVmTypesByName, template, instanceGroup);
        }
        return filteredInstanceTypes;
    }

    private List<String> promoteFirstVolumeCompatibleInstanceType(List<String> instanceTypes, Map<String, VmType> vmTypesByName, Template template,
            InstanceGroup instanceGroup) {
        Optional<String> compatibleInstanceType = instanceTypes.stream()
                .filter(instanceType -> supportsVolumes(vmTypesByName.get(instanceType), template, true))
                .findFirst();
        if (compatibleInstanceType.isEmpty()) {
            LOGGER.info("None of the available instance types {} of group {} support the volumes of the group, {} will be the new default instance type.",
                    instanceTypes, instanceGroup.getGroupName(), instanceTypes.getFirst());
            return instanceTypes;
        }
        String newDefaultInstanceType = compatibleInstanceType.get();
        if (newDefaultInstanceType.equals(instanceTypes.getFirst())) {
            return instanceTypes;
        }
        LOGGER.info("Instance types {} of group {} do not support the volumes of the group, {} will be the new default instance type.",
                instanceTypes.subList(0, instanceTypes.indexOf(newDefaultInstanceType)), instanceGroup.getGroupName(), newDefaultInstanceType);
        List<String> reorderedInstanceTypes = new ArrayList<>();
        reorderedInstanceTypes.add(newDefaultInstanceType);
        instanceTypes.stream().filter(instanceType -> !instanceType.equals(newDefaultInstanceType)).forEach(reorderedInstanceTypes::add);
        return reorderedInstanceTypes;
    }

    private void updateInstanceTypes(List<String> filteredInstanceTypes, Set<VmType> availableVmTypes, Template template,
            InstanceGroup instanceGroup, Stack stack, String locationString) {
        String newInstanceType = filteredInstanceTypes.getFirst();
        List<String> originalFallbackInstanceTypes = template.getFallbackInstanceTypesAsList();
        List<String> droppedFallbacks = originalFallbackInstanceTypes.stream()
                .filter(type -> !filteredInstanceTypes.contains(type))
                .distinct()
                .toList();
        if (!droppedFallbacks.isEmpty()) {
            LOGGER.info("Fallback instance types {} are not available at location {} for group {}, dropping them from the template.",
                    droppedFallbacks, locationString, instanceGroup.getGroupName());
            fireInstanceGroupEvent(stack, instanceGroup, STACK_INSTANCE_TYPE_VALIDATION_FILTERED, instanceGroup.getGroupName(), locationString,
                    String.join(", ", droppedFallbacks));
        }

        if (!newInstanceType.equals(template.getInstanceType())) {
            changeDefaultInstanceTypeAndAlignVolumeTypes(newInstanceType, availableVmTypes, template, instanceGroup, stack, locationString);
        }

        List<String> newFallbackInstanceTypes = filterFallbacksByVolumeTypeSupport(filteredInstanceTypes.subList(1, filteredInstanceTypes.size()),
                availableVmTypes, template, instanceGroup, stack);
        if (!newFallbackInstanceTypes.equals(originalFallbackInstanceTypes)) {
            template.setFallbackInstanceTypes(new Json(newFallbackInstanceTypes));
        }
    }

    private void changeDefaultInstanceTypeAndAlignVolumeTypes(String newInstanceType, Set<VmType> availableVmTypes, Template template,
            InstanceGroup instanceGroup, Stack stack, String locationString) {
        LOGGER.info("Changing the default instance type of group {} from {} to {} at location {}.",
                instanceGroup.getGroupName(), template.getInstanceType(), newInstanceType, locationString);
        fireInstanceGroupEvent(stack, instanceGroup, STACK_INSTANCE_TYPE_VALIDATION_DEFAULT_CHANGED, instanceGroup.getGroupName(),
                template.getInstanceType(), locationString, newInstanceType);
        VmType newVmType = availableVmTypes.stream().filter(type -> type.value().equals(newInstanceType)).findFirst().orElseThrow();
        template.setInstanceType(newInstanceType);
        alignRootVolumeTypeWithInstanceType(template, newVmType, instanceGroup, stack);
        alignVolumeTypesWithInstanceType(template, newVmType, instanceGroup, stack);
    }

    private List<String> filterFallbacksByVolumeTypeSupport(List<String> fallbackInstanceTypes, Set<VmType> availableVmTypes, Template template,
            InstanceGroup instanceGroup, Stack stack) {
        if (fallbackInstanceTypes.isEmpty()) {
            return fallbackInstanceTypes;
        }
        Map<String, VmType> vmTypesByName = availableVmTypes.stream()
                .collect(Collectors.toMap(StringType::value, Function.identity(), (first, second) -> first));
        List<String> supportedFallbacks = fallbackInstanceTypes.stream()
                .filter(instanceType -> supportsVolumes(vmTypesByName.get(instanceType), template, false))
                .toList();
        List<String> droppedFallbacks = fallbackInstanceTypes.stream().filter(type -> !supportedFallbacks.contains(type)).toList();
        if (!droppedFallbacks.isEmpty()) {
            String volumeTypes = getVolumeTypesOfTemplate(template);
            LOGGER.info("Fallback instance types {} of group {} do not support the volumes ({}) of instance type {}, dropping them from the template.",
                    droppedFallbacks, instanceGroup.getGroupName(), volumeTypes, template.getInstanceType());
            fireInstanceGroupEvent(stack, instanceGroup, STACK_INSTANCE_TYPE_VALIDATION_FALLBACK_DISK_INCOMPATIBLE, instanceGroup.getGroupName(),
                    template.getInstanceType(), volumeTypes, String.join(", ", droppedFallbacks));
        }
        return supportedFallbacks;
    }

    private String getVolumeTypesOfTemplate(Template template) {
        List<String> volumeTypes = new ArrayList<>();
        if (!isEmpty(template.getRootVolumeType())) {
            volumeTypes.add(template.getRootVolumeType() + " (root)");
        }
        Optional.ofNullable(template.getVolumeTemplates()).orElse(Set.of()).stream()
                .filter(this::isNotEmptyVolumeSet)
                .map(VolumeTemplate::getVolumeType)
                .distinct()
                .forEach(volumeTypes::add);
        return String.join(", ", volumeTypes);
    }

    private boolean supportsVolumes(VmType vmType, Template template, boolean exchangeable) {
        boolean rootVolumeToCheck = !exchangeable && !isEmpty(template.getRootVolumeType());
        boolean dataVolumesToCheck = Optional.ofNullable(template.getVolumeTemplates()).orElse(Set.of()).stream().anyMatch(this::isNotEmptyVolumeSet);
        if (isVolumeSupportUnknown(vmType) || !(rootVolumeToCheck || dataVolumesToCheck)) {
            return true;
        }
        Map<String, VolumeParameterType> platformDiskMappings = diskMappings.get().getOrDefault(Platform.platform(template.getCloudPlatform()), Map.of());
        return (!rootVolumeToCheck || supportsRootVolumeType(vmType, template, platformDiskMappings))
                && supportsDataVolumes(vmType, template, platformDiskMappings, exchangeable);
    }

    private boolean supportsRootVolumeType(VmType vmType, Template template, Map<String, VolumeParameterType> platformDiskMappings) {
        VolumeParameterType rootVolumeParameterType = volumeParameterType(platformDiskMappings, template.getRootVolumeType());
        return rootVolumeParameterType == null || vmType.getVolumeParameterbyVolumeParameterType(rootVolumeParameterType) != null;
    }

    private boolean supportsDataVolumes(VmType vmType, Template template, Map<String, VolumeParameterType> platformDiskMappings, boolean exchangeable) {
        Set<VolumeTemplate> volumeTemplates = Optional.ofNullable(template.getVolumeTemplates()).orElse(Set.of()).stream()
                .filter(this::isNotEmptyVolumeSet)
                .collect(Collectors.toSet());
        int persistentVolumeSize = 0;
        for (VolumeTemplate volumeTemplate : volumeTemplates) {
            VolumeParameterType volumeParameterType = volumeParameterType(platformDiskMappings, volumeTemplate.getVolumeType());
            if (volumeParameterType == null) {
                continue;
            }
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config == null && exchangeable && isExchangeablePersistentVolumeType(volumeParameterType)) {
                config = Optional.ofNullable(volumeParameterType(platformDiskMappings, defaultVolumeType(vmType)))
                        .map(vmType::getVolumeParameterbyVolumeParameterType)
                        .orElse(null);
            }
            if (config == null || !isWithinVolumeLimits(volumeTemplate, config)) {
                return false;
            }
            if (!volumeParameterType.in(VolumeParameterType.EPHEMERAL, VolumeParameterType.LOCAL_SSD)) {
                persistentVolumeSize += volumeTemplate.getVolumeSize() * volumeTemplate.getVolumeCount();
            }
        }
        Object maximumPersistentVolumeSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB);
        return maximumPersistentVolumeSize == null || persistentVolumeSize <= Integer.parseInt(maximumPersistentVolumeSize.toString());
    }

    private boolean isWithinVolumeLimits(VolumeTemplate volumeTemplate, VolumeParameterConfig config) {
        boolean countSupported = CollectionUtils.isNotEmpty(config.possibleNumberValues())
                ? config.possibleNumberValues().contains(volumeTemplate.getVolumeCount())
                : config.maximumNumber() == null || volumeTemplate.getVolumeCount() <= config.maximumNumber();
        boolean sizeSupported = CollectionUtils.isNotEmpty(config.possibleSizeValues())
                ? config.possibleSizeValues().contains(volumeTemplate.getVolumeSize())
                : config.maximumSize() == null || volumeTemplate.getVolumeSize() <= config.maximumSize();
        return countSupported && sizeSupported;
    }

    private VolumeParameterType volumeParameterType(Map<String, VolumeParameterType> platformDiskMappings, String volumeType) {
        return isEmpty(volumeType) ? null : platformDiskMappings.get(volumeType);
    }

    private boolean isNotEmptyVolumeSet(VolumeTemplate volumeTemplate) {
        return volumeTemplate.getVolumeCount() != null && volumeTemplate.getVolumeCount() != 0
                && volumeTemplate.getVolumeSize() != null && volumeTemplate.getVolumeSize() != 0;
    }

    private boolean isVolumeSupportUnknown(VmType vmType) {
        return vmType == null || !vmType.isMetaSet();
    }

    private void alignRootVolumeTypeWithInstanceType(Template template, VmType newVmType, InstanceGroup instanceGroup, Stack stack) {
        if (isEmpty(template.getRootVolumeType()) || isVolumeSupportUnknown(newVmType)) {
            return;
        }
        Map<String, VolumeParameterType> platformDiskMappings = diskMappings.get().getOrDefault(Platform.platform(template.getCloudPlatform()), Map.of());
        if (supportsRootVolumeType(newVmType, template, platformDiskMappings)) {
            return;
        }
        String defaultRootVolumeType = defaultRootVolumeType(template, newVmType.value(), List.of());
        if (isEmpty(defaultRootVolumeType) || defaultRootVolumeType.equals(template.getRootVolumeType())) {
            LOGGER.debug("Instance type {} does not define a different default root volume type, leaving the root volume type of group {} untouched.",
                    newVmType.value(), instanceGroup.getGroupName());
            return;
        }
        LOGGER.info("Root volume type {} is not supported by instance type {}, changing it to {} in group {}.",
                template.getRootVolumeType(), newVmType.value(), defaultRootVolumeType, instanceGroup.getGroupName());
        fireInstanceGroupEvent(stack, instanceGroup, STACK_INSTANCE_TYPE_VALIDATION_ROOT_VOLUME_TYPE_CHANGED, instanceGroup.getGroupName(),
                template.getRootVolumeType(), newVmType.value(), defaultRootVolumeType);
        template.setRootVolumeType(defaultRootVolumeType);
    }

    private void alignVolumeTypesWithInstanceType(Template template, VmType newVmType, InstanceGroup instanceGroup, Stack stack) {
        if (CollectionUtils.isEmpty(template.getVolumeTemplates())) {
            return;
        }
        String defaultVolumeType = defaultVolumeType(newVmType);
        if (isEmpty(defaultVolumeType)) {
            LOGGER.debug("Instance type {} does not define a default volume type, leaving the volume types of group {} untouched.",
                    newVmType.value(), instanceGroup.getGroupName());
            return;
        }
        Map<String, VolumeParameterType> platformDiskMappings = diskMappings.get().getOrDefault(Platform.platform(template.getCloudPlatform()), Map.of());
        for (VolumeTemplate volumeTemplate : template.getVolumeTemplates()) {
            VolumeParameterType volumeParameterType = platformDiskMappings.get(volumeTemplate.getVolumeType());
            if (!isExchangeablePersistentVolumeType(volumeParameterType)) {
                continue;
            }
            if (newVmType.getVolumeParameterbyVolumeParameterType(volumeParameterType) == null) {
                LOGGER.info("Volume type {} is not supported by instance type {}, changing it to {} in group {}.",
                        volumeTemplate.getVolumeType(), newVmType.value(), defaultVolumeType, instanceGroup.getGroupName());
                fireInstanceGroupEvent(stack, instanceGroup, STACK_INSTANCE_TYPE_VALIDATION_VOLUME_TYPE_CHANGED, instanceGroup.getGroupName(),
                        volumeTemplate.getVolumeType(), newVmType.value(), defaultVolumeType);
                volumeTemplate.setVolumeType(defaultVolumeType);
                increaseVolumeSizeToMinimum(volumeTemplate, newVmType, platformDiskMappings, instanceGroup, stack);
            }
        }
    }

    private boolean isExchangeablePersistentVolumeType(VolumeParameterType volumeParameterType) {
        return volumeParameterType != null && !volumeParameterType.in(VolumeParameterType.LOCAL_SSD, VolumeParameterType.EPHEMERAL);
    }

    private void setRootVolumeTypeIfEmpty(Template template, VmType vmType, List<String> fallbackInstanceTypes, InstanceGroup instanceGroup) {
        if (isEmpty(template.getRootVolumeType())) {
            String defaultRootVolumeType = defaultRootVolumeType(template, vmType.value(), fallbackInstanceTypes);
            if (!isEmpty(defaultRootVolumeType)) {
                LOGGER.info("Setting the root volume type of group {} to {} based on instance type {} and fallback instance types {}.",
                        instanceGroup.getGroupName(), defaultRootVolumeType, vmType.value(), fallbackInstanceTypes);
                template.setRootVolumeType(defaultRootVolumeType);
            }
        }
    }

    private String defaultRootVolumeType(Template template, String instanceType, List<String> fallbackInstanceTypes) {
        PlatformParameters pps = Optional.ofNullable(platformParameters.get())
                .map(parameters -> parameters.get(Platform.platform(template.getCloudPlatform())))
                .orElse(null);
        if (pps == null) {
            return null;
        }
        DiskType rootDiskType;
        try {
            rootDiskType = pps.defaultRootDiskType(instanceType, fallbackInstanceTypes);
        } catch (CloudbreakServiceException e) {
            LOGGER.info("Instance type {} and fallback instance types {} do not have a common root volume type, using the one of {}. Reason: {}",
                    instanceType, fallbackInstanceTypes, instanceType, e.getMessage());
            rootDiskType = pps.defaultRootDiskType(instanceType);
        }
        return rootDiskType == null ? null : rootDiskType.value();
    }

    private String defaultVolumeType(VmType vmType) {
        if (!vmType.isMetaSet()) {
            return null;
        }
        return readDefaultDiskTypeAsString(vmType.getMetaData());
    }

    private String readDefaultDiskTypeAsString(VmTypeMeta vmTypeMeta) {
        return (String) vmTypeMeta.getProperties().get(VmTypeMeta.DEFAULT_DISK_TYPE);
    }

    private void increaseVolumeSizeToMinimum(VolumeTemplate volumeTemplate, VmType vmType, Map<String, VolumeParameterType> platformDiskMappings,
            InstanceGroup instanceGroup, Stack stack) {
        VolumeParameterType newVolumeParameterType = platformDiskMappings.get(volumeTemplate.getVolumeType());
        if (newVolumeParameterType == null) {
            return;
        }
        VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(newVolumeParameterType);
        if (config != null && config.minimumSize() != null && volumeTemplate.getVolumeSize() != null
                && volumeTemplate.getVolumeSize() < config.minimumSize()) {
            LOGGER.info("Increasing the volume size of group {} from {} GB to the minimum {} GB required by volume type {}.",
                    instanceGroup.getGroupName(), volumeTemplate.getVolumeSize(), config.minimumSize(), volumeTemplate.getVolumeType());
            fireInstanceGroupEvent(stack, instanceGroup, STACK_INSTANCE_TYPE_VALIDATION_VOLUME_SIZE_INCREASED, instanceGroup.getGroupName(),
                    volumeTemplate.getVolumeType(), String.valueOf(volumeTemplate.getVolumeSize()), String.valueOf(config.minimumSize()));
            volumeTemplate.setVolumeSize(config.minimumSize());
        }
    }

    private void fireInstanceGroupEvent(Stack stack, InstanceGroup instanceGroup, ResourceEvent resourceEvent, String... eventMessageArgs) {
        String eventType = stack.getStatus() == null ? "UNKNOWN" : stack.getStatus().name();
        cloudbreakEventService.fireCloudbreakInstanceGroupEvent(stack.getId(), eventType, instanceGroup.getGroupName(), resourceEvent,
                Arrays.asList(eventMessageArgs));
    }

    private VmType selectVmTypeOrNull(List<String> possibleInstanceTypes, Set<VmType> availableVmTypes, Set<VmType> vmTypesOfLocation) {
        VmType vmType = selectVmTypeOrNull(possibleInstanceTypes, availableVmTypes);
        return vmType == null ? selectVmTypeOrNull(possibleInstanceTypes, vmTypesOfLocation) : vmType;
    }

    private VmType selectVmTypeOrNull(List<String> possibleInstanceTypes, Set<VmType> availableVmTypes) {
        for (String instanceType : possibleInstanceTypes) {
            for (VmType type : availableVmTypes) {
                if (type.value().equals(instanceType)) {
                    return type;
                }
            }
        }
        return null;
    }

    public void validateGroupForVerticalScale(Credential credential, InstanceGroup instanceGroup, Stack stack,
            CdpResourceType stackType, ValidationResult.ValidationResultBuilder validationBuilder) {
        Template template = instanceGroup.getTemplate();
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                stack.getRegion(),
                stack.getPlatformVariant(),
                stackType,
                Map.of(ARCHITECTURE, stack.getArchitectureName()));
        Platform platform = Platform.platform(template.getCloudPlatform());
        Map<String, Set<VmType>> machines = cloudVmTypes.getCloudVmResponses();
        String locationString = locationService.location(stack.getRegion(), stack.getAvailabilityZone());
        VmType vmType = Optional.ofNullable(machines.get(locationString))
                .orElse(Collections.emptySet())
                .stream()
                .filter(type -> type.value().equals(template.getInstanceType()))
                .findFirst()
                .orElse(null);
        validateVolumeTemplates(template, vmType, platform, validationBuilder, instanceGroup, stack);
        validateMaximumVolumeSize(template, vmType, validationBuilder);
    }

    private boolean isArchitectureMatching(VmType vmType, Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        Architecture vmArchitecture = vmType.getMetaData().getArchitecture();
        Architecture stackArchitecture = stack.getArchitecture();
        if (vmArchitecture != stackArchitecture) {
            validationBuilder.error(
                    String.format("The '%s' instance type's architecture '%s' is not matching requested architecture '%s'",
                            vmType.value(), vmArchitecture.getName(), stackArchitecture.getName()));
            return false;
        }
        return true;
    }

    private boolean isHyperVGenerationSupported(VmType vmType, String imageGeneration, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (imageGeneration != null && !vmType.getMetaData().isHyperVGenerationSupported(imageGeneration)) {
            validationBuilder.error(
                    String.format("The '%s' instance type does not support the selected image's Hyper-V generation '%s'. Supported generation(s): '%s'",
                            vmType.value(), imageGeneration, vmType.getMetaData().getHyperVGenerations().orElse("unknown")));
            return false;
        }
        return true;
    }

    private void validateVolumeTemplates(Template value, VmType vmType, Platform platform,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        for (VolumeTemplate volumeTemplate : value.getVolumeTemplates()) {
            Map<Platform, Map<String, VolumeParameterType>> disks = diskMappings.get();
            Optional<VolumeParameterType> volumeParameterType = Optional.ofNullable(disks.get(platform))
                    .flatMap(platformDisks -> Optional.ofNullable(platformDisks.get(volumeTemplate.getVolumeType())));

            if (volumeParameterType.isEmpty()) {
                validationBuilder.error(String.format("The '%s' volume type isn't supported by '%s' platform",
                        volumeTemplate.getVolumeType(), platform.value()));
            }

            validateVolume(volumeTemplate, vmType, platform, volumeParameterType.orElse(null), validationBuilder, instanceGroup, stack);
        }
    }

    private void validateCustomInstanceType(Template template, ValidationResult.ValidationResultBuilder validationBuilder) {
        Map<String, Object> params = template.getAttributes().getMap();
        Platform platform = Platform.platform(template.getCloudPlatform());
        PlatformParameters pps = platformParameters.get().get(platform);
        if (pps != null) {
            Boolean customInstanceType = pps.specialParameters().getSpecialParameters().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE);
            if (BooleanUtils.isTrue(customInstanceType)) {
                if (params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS) == null
                        || params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY) == null) {
                    validationBuilder.error(String.format("Missing 'cpus' or 'memory' param for custom instancetype on %s platform",
                            template.getCloudPlatform()));
                }
            } else {
                validationBuilder.error(String.format("Custom instancetype is not supported on %s platform", template.getCloudPlatform()));
            }
        }
    }

    private void validateVolume(VolumeTemplate value, VmType vmType, Platform platform, VolumeParameterType volumeParameterType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        validateVolumeType(value, platform, validationBuilder);
        validateVolumeCount(value, vmType, volumeParameterType, validationBuilder, instanceGroup, stack);
        validateVolumeSize(value, vmType, volumeParameterType, validationBuilder, instanceGroup, stack);
    }

    private void validateMaximumVolumeSize(Template value, VmType vmType, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (vmType != null) {
            Object maxSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB);
            if (maxSize != null) {
                int fullSize = value.getVolumeTemplates().stream()
                        .filter(volumeTemplate -> !AwsDiskType.Ephemeral.value().equals(volumeTemplate.getVolumeType()))
                        .mapToInt(volume -> volume.getVolumeSize() * volume.getVolumeCount()).sum();
                if (Integer.parseInt(maxSize.toString()) < fullSize) {
                    validationBuilder.error(
                            String.format("The %s platform does not support %s Gb full volume size. The maximum size of disks could be %s Gb.",
                                    value.getCloudPlatform(), fullSize, maxSize));
                }
            }
        }
    }

    private void validateVolumeType(VolumeTemplate value, Platform platform, ValidationResult.ValidationResultBuilder validationBuilder) {
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            if (!diskTypes.get(platform).contains(diskType)) {
                validationBuilder.error(String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value()));
            }
        }
    }

    private boolean isIDBrokerInstanceGroup(InstanceGroup instanceGroup, Stack stack) {
        return GROUP_NAME_ID_BROKER.equalsIgnoreCase(instanceGroup.getGroupName()) && datalakeOrTemplate(stack.getType());
    }

    private boolean isCoordinatorAndExecutorInstanceGroup(String bluePrintText) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(bluePrintText);
        Set<ServiceComponent> service = new HashSet<>();
        if (processor != null) {
            service = processor.getAllComponents().stream()
                    .filter(serviceComponent -> ROLE_IMPALAD.equals(serviceComponent.getComponent())).collect(Collectors.toSet());
        }
        if (!service.isEmpty()) {
            return true;
        }
        return false;
    }

    private void validateVolumeCount(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount()) && volumeParameterType != null) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                validateVolumeCountInParameterConfig(config, value, vmType, validationBuilder, instanceGroup, stack);
            } else {
                validationBuilder.error(String.format("The '%s' instance type does not support '%s' volume type", vmType.value(), volumeParameterType));
            }
        }
    }

    private void validateVolumeCountInParameterConfig(VolumeParameterConfig config, VolumeTemplate value, VmType vmType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        // IDBroker does not use data volume, so its volume count should be zero
        // To be backward-compatible, we only check max limit and allow the min limit to be zero for IDBroker
        if (!config.possibleNumberValues().isEmpty()) {
            if (!config.possibleNumberValues().contains(value.getVolumeCount())) {
                validationBuilder.error(String.format("Allowed volume count(s) for '%s': %s", vmType.value(), config.possibleNumberValues()));
            }
        } else if (value.getVolumeCount() > config.maximumNumber()) {
            validationBuilder.error(String.format("Max allowed volume count for '%s': %s", vmType.value(), config.maximumNumber()));
        } else if (shouldValidateBasedOnGroupName(instanceGroup, stack) &&
                value.getVolumeCount() < config.minimumNumber()) {
            validationBuilder.error(String.format("Min allowed volume count for '%s': %s", vmType.value(), config.minimumNumber()));
        }
    }

    private boolean isSdxComputeInstanceHostGroup(InstanceGroup instanceGroup, StackType stackType) {
        return SDX_COMPUTE_INSTANCES.contains(instanceGroup.getGroupName().toLowerCase(Locale.ROOT)) &&
                datalakeOrTemplate(stackType);
    }

    private static boolean datalakeOrTemplate(StackType stackType) {
        return StackType.DATALAKE.equals(stackType) || StackType.TEMPLATE.equals(stackType);
    }

    private void validateVolumeSize(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount()) && volumeParameterType != null) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                validateVolumeSizeInParameterConfig(config, value, vmType, validationBuilder, instanceGroup, stack);
            } else {
                validationBuilder.error(String.format("The '%s' instance type does not support '%s' volume type", vmType.value(), volumeParameterType));
            }
        }
    }

    private void validateVolumeSizeInParameterConfig(VolumeParameterConfig config, VolumeTemplate value, VmType vmType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        // IDBroker does not use data volume, so its volume size should be zero
        // To be backward-compatible, we only check max limit and allow the min limit to be zero for IDBroker
        if (!config.possibleSizeValues().isEmpty()) {
            if (!config.possibleSizeValues().contains(value.getVolumeSize())) {
                validationBuilder.error(String.format("Allowed volume size(s) for '%s': %s in case of %s volumes and the current size is %s.",
                        vmType.value(),
                        config.possibleSizeValues(),
                        value.getVolumeType(),
                        value.getVolumeSize()));
            }
        } else if (value.getVolumeSize() > config.maximumSize()) {
            validationBuilder.error(String.format("Max allowed volume size for '%s': %s GB in case of %s volumes and the current size is %s GB.",
                    vmType.value(),
                    config.maximumSize(),
                    value.getVolumeType(),
                    value.getVolumeSize()));
        } else if (shouldValidateBasedOnGroupName(instanceGroup, stack)
                && value.getVolumeSize() < config.minimumSize()) {
            validationBuilder.error(String.format("Min allowed volume size for '%s': %s GB in case of %s volumes and the current size is %s GB.",
                    vmType.value(),
                    config.minimumSize(),
                    value.getVolumeType(),
                    value.getVolumeSize()));
        }
    }

    private boolean shouldValidateBasedOnGroupName(InstanceGroup instanceGroup, Stack stack) {
        return !(isIDBrokerInstanceGroup(instanceGroup, stack)
                || isSdxComputeInstanceHostGroup(instanceGroup, stack.getType())
                || isCoordinatorAndExecutorInstanceGroup(stack.getBlueprintJsonText()));
    }

    private boolean needToCheckVolume(VolumeParameterType volumeParameterType, Object value) {
        return volumeParameterType != null && volumeParameterType != VolumeParameterType.EPHEMERAL || value != null;
    }

    private String getInvalidVmTypeErrorMessage(List<String> instanceTypes, String platform, String region) {
        String baseMsg = "Our platform currently not supporting the '%s' instance types for '%s' in %s.";
        if (isEmpty(supportedVmTypesDocPageLink)) {
            return String.format(baseMsg, instanceTypes, platform, region);
        }
        return String.format("Our platform currently not supporting the '%s' instance types for '%s' in %s." +
                " You can find the supported types here: %s", instanceTypes, platform, region, supportedVmTypesDocPageLink);
    }

    private record InstanceTypeAvailability(Set<VmType> vmTypes, String location) {
    }
}
