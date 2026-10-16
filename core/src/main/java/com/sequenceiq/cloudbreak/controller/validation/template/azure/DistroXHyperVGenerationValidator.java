package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.util.AzureHyperVGenerationUtil;
import com.sequenceiq.cloudbreak.validation.HyperVGenerationCompatibilityUtil;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class DistroXHyperVGenerationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXHyperVGenerationValidator.class);

    @Inject
    private CredentialConverter credentialConverter;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private LocationService locationService;

    @Inject
    private EntitlementService entitlementService;

    /**
     * Rejects an Azure image / instance type Hyper-V generation mismatch of a Data Hub at request time, so the user gets a 400 instead of a provisioning
     * failure minutes later. When instance type fallback is enabled for the account, the fallback instance types are checked too, as the deployment may
     * switch to any of them after a capacity error. Deliberately fails open: an image without the generation tag, or an instance type whose supported
     * generations Azure did not report, is not blocked here - the provisioning time validation is the second line of defence.
     */
    public void validate(StackV4Request stackRequest, Stack stack, DetailedEnvironmentResponse environment, StatedImage image, StackType stackType) {
        if (!StackType.WORKLOAD.equals(stackType) || !CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform()) || image == null
                || image.getImage() == null || CollectionUtils.isEmpty(stackRequest.getInstanceGroups())) {
            return;
        }
        Optional<String> imageGeneration = AzureHyperVGenerationUtil.toGeneration(image.getImage().getTags());
        if (imageGeneration.isEmpty()) {
            return;
        }
        String generation = imageGeneration.get();
        try {
            Map<String, VmType> vmTypesByName = getVmTypesByName(credentialConverter.convert(environment.getCredential()), stack);
            boolean fallbackEnabled = entitlementService.isFallbackInstanceTypeEnabled(environment.getAccountId());
            HyperVGenerationCompatibilityUtil.validate(stackRequest, generation, instanceType -> getSupportedGenerations(vmTypesByName.get(instanceType)),
                    fallbackEnabled);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Could not validate the Hyper-V generation of the instance types of stack {}. Proceeding without validation.", stack.getName(), e);
        }
    }

    private Map<String, VmType> getVmTypesByName(Credential credential, Stack stack) {
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                stack.getRegion(),
                stack.getPlatformVariant(),
                CdpResourceType.DATAHUB,
                Map.of(ARCHITECTURE, stack.getArchitectureName()));
        String location = locationService.location(stack.getRegion(), stack.getAvailabilityZone());
        return Optional.ofNullable(cloudVmTypes.getCloudVmResponses())
                .map(responses -> responses.get(location))
                .orElse(Set.of())
                .stream()
                .collect(Collectors.toMap(VmType::value, Function.identity(), (first, second) -> first));
    }

    private Optional<String> getSupportedGenerations(VmType vmType) {
        return Optional.ofNullable(vmType)
                .map(VmType::getMetaData)
                .flatMap(VmTypeMeta::getHyperVGenerations);
    }
}
