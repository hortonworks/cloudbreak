package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.FreeIpaRecommendationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.instance.VmTypeToVmTypeResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@Service
public class FreeIpaRecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRecommendationService.class);

    @Inject
    private CredentialService credentialService;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Inject
    private VmTypeToVmTypeResponseConverter vmTypeConverter;

    public FreeIpaRecommendationResponse getRecommendation(String credentialCrn, String region, String availabilityZone, String architecture) {
        Credential credential = credentialService.getCredentialByCredCrn(credentialCrn);
        Architecture architectureEnum = Architecture.fromStringWithFallback(architecture);
        String defaultInstanceType = defaultInstanceTypeProvider.getForPlatform(credential.getCloudPlatform(), architectureEnum);
        Set<VmType> availableAllVmTypes = getAvailableAllVmTypes(region, availabilityZone, credential, architectureEnum);

        Optional<VmType> defaultVmType = availableAllVmTypes.stream()
                .filter(vmType -> defaultInstanceType.equals(vmType.value()))
                .findAny();
        Set<VmTypeResponse> availableVmTypes = availableAllVmTypes.stream()
                .filter(vmType -> filterVmTypeLargerThanDefault(vmType, defaultVmType))
                .map(vmType -> vmTypeConverter.convert(vmType))
                .collect(Collectors.toSet());

        return new FreeIpaRecommendationResponse(availableVmTypes, defaultInstanceType);
    }

    private Set<VmType> getAvailableAllVmTypes(String region, String availabilityZone, Credential credential, Architecture architecture) {
        CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                region,
                credential.getCloudPlatform(),
                CdpResourceType.DEFAULT,
                Map.of(ARCHITECTURE, Optional.ofNullable(architecture).orElse(Architecture.X86_64).getName()));

        Set<VmType> availableVmTypes = Collections.emptySet();
        if (vmTypes.getCloudVmResponses() != null && StringUtils.isNotBlank(availabilityZone)
                && vmTypes.getDefaultCloudVmResponses().containsKey(availabilityZone)) {
            availableVmTypes = vmTypes.getCloudVmResponses().get(availabilityZone);
        } else if (vmTypes.getCloudVmResponses() != null && !vmTypes.getCloudVmResponses().isEmpty()) {
            availableVmTypes = vmTypes.getCloudVmResponses().values().iterator().next();
        }
        return availableVmTypes;
    }

    private Set<VmTypeResponse> filterVmTypeLargerThanDefault(String defaultInstanceType, Set<VmType> availableVmTypes) {
        Optional<VmType> defaultVmType = availableVmTypes.stream()
                .filter(vmType -> defaultInstanceType.equals(vmType.getValue()))
                .findAny();
        return availableVmTypes.stream()
                .filter(vmType -> filterVmTypeLargerThanDefault(vmType, defaultVmType))
                .map(vmType -> vmTypeConverter.convert(vmType))
                .collect(Collectors.toSet());
    }

    private boolean filterVmTypeLargerThanDefault(VmType vmType, Optional<VmType> defaultVmType) {
        if (defaultVmType.isEmpty() || !defaultVmType.get().isMetaSet() || !vmType.isMetaSet()) {
            return false;
        }
        VmTypeMeta defaultVmTypeMetaData = defaultVmType.get().getMetaData();
        VmTypeMeta vmTypeMetaData = vmType.getMetaData();
        if (defaultVmTypeMetaData.getCPU() == null || defaultVmTypeMetaData.getMemoryInGb() == null
                || vmTypeMetaData.getCPU() == null || vmTypeMetaData.getMemoryInGb() == null) {
            return false;
        }
        return vmTypeMetaData.getCPU() >= defaultVmTypeMetaData.getCPU() && vmTypeMetaData.getMemoryInGb() >= defaultVmTypeMetaData.getMemoryInGb();
    }

    public void validateCustomInstanceType(Stack stack, Credential credential) {
        String defaultInstanceType = defaultInstanceTypeProvider.getForPlatform(stack.getCloudPlatform(), stack.getArchitecture());
        Map<String, String> customInstanceTypes = getCustomInstanceTypes(stack, defaultInstanceType);
        if (!customInstanceTypes.isEmpty()) {
            Set<VmType> availableAllVmTypes = getAvailableAllVmTypes(
                    stack.getRegion(),
                    stack.getAvailabilityZone(),
                    credential,
                    stack.getArchitecture());
            Set<VmTypeResponse> availableMinimumRequiredVmTypes = filterVmTypeLargerThanDefault(
                    defaultInstanceType,
                    availableAllVmTypes);
            Optional<VmType> defaultVmType = availableAllVmTypes.stream()
                    .filter(vmType -> defaultInstanceType.equals(vmType.getValue()))
                    .findAny();

            Set<String> allVmTypes = availableAllVmTypes.stream()
                    .map(VmType::getValue)
                    .collect(Collectors.toSet());
            Set<String> minimumRequiredVmTypes = availableMinimumRequiredVmTypes.stream()
                    .map(VmTypeResponse::getValue)
                    .collect(Collectors.toSet());

            customInstanceTypes.forEach((instanceGroup, instanceType) -> {
                if (!allVmTypes.contains(instanceType)) {
                    String message = String.format("Invalid custom instance type for FreeIPA: %s - %s. " +
                            "The instance type is not available in %s.",
                            instanceGroup,
                            instanceType,
                            stack.getRegion());
                    LOGGER.warn(message);
                    throw new BadRequestException(message);
                }
                if (!minimumRequiredVmTypes.contains(instanceType)) {
                    String message = String.format("Invalid custom instance type for FreeIPA: %s - %s. " +
                            "The instance type needs at least %s MB Memory and %s VCPU.",
                            instanceGroup,
                            instanceType,
                            defaultVmType.get().getMetaData().getMemoryInGb(),
                            defaultVmType.get().getMetaData().getCPU());
                    LOGGER.warn(message);
                    throw new BadRequestException(message);
                }
            });
        }
    }

    private Map<String, String> getCustomInstanceTypes(Stack stack, String defaultInstanceType) {
        return stack.getInstanceGroups().stream()
                .filter(ig -> ObjectUtils.allNotNull(ig, ig.getTemplate(), ig.getTemplate().getInstanceType()))
                .filter(ig -> !ig.getTemplate().getInstanceType().equals(defaultInstanceType))
                .collect(Collectors.toMap(ig -> ig.getGroupName(), ig -> ig.getTemplate().getInstanceType()));
    }
}
