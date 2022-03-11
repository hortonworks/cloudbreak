package com.sequenceiq.freeipa.service.stack;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.FreeIpaRecommendationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.VmTypeResponse;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.instance.VmTypeToVmTypeResponseConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceTypeProvider;

@Service
public class FreeIpaRecommendationService {

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

    public FreeIpaRecommendationResponse getRecommendation(String credentialCrn, String region, String availabilityZone) {
        Credential credential = credentialService.getCredentialByCredCrn(credentialCrn);
        String defaultInstanceType = defaultInstanceTypeProvider.getForPlatform(credential.getCloudPlatform());
        Set<VmTypeResponse> availableVmTypes = getAvailableVmTypes(region, availabilityZone, credential, defaultInstanceType);
        return new FreeIpaRecommendationResponse(availableVmTypes, defaultInstanceType);
    }

    private Set<VmTypeResponse> getAvailableVmTypes(String region, String availabilityZone, Credential credential, String defaultInstanceType) {
        CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(extendedCloudCredentialConverter.convert(credential),
                region, credential.getCloudPlatform(), CdpResourceType.DEFAULT, Maps.newHashMap());

        Set<VmType> availableVmTypes = Collections.emptySet();
        if (vmTypes.getCloudVmResponses() != null && StringUtils.isNotBlank(availabilityZone)) {
            availableVmTypes = vmTypes.getCloudVmResponses().get(availabilityZone);
        } else if (vmTypes.getCloudVmResponses() != null && !vmTypes.getCloudVmResponses().isEmpty()) {
            availableVmTypes = vmTypes.getCloudVmResponses().values().iterator().next();
        }

        Optional<VmType> defaultVmType = availableVmTypes.stream()
                .filter(vmType -> defaultInstanceType.equals(vmType.value()))
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
}
