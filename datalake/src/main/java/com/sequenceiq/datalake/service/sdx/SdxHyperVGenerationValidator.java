package com.sequenceiq.datalake.service.sdx;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.AzureHyperVGenerationUtil;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.VmTypeMetaJson;
import com.sequenceiq.sdx.api.model.VmTypeResponse;

@Component
public class SdxHyperVGenerationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxHyperVGenerationValidator.class);

    @Inject
    private SdxRecommendationService sdxRecommendationService;

    @Inject
    private EntitlementService entitlementService;

    /**
     * Rejects an Azure image / instance type Hyper-V generation mismatch at request time, so the user gets a 400 instead of a provisioning failure minutes
     * later. When instance type fallback is enabled for the account, the fallback instance types are checked too, as the deployment may switch to any
     * of them after a capacity error. Deliberately fails open: an image without the generation tag, or an instance type whose supported generations
     * Azure did not report, is not blocked here - the provisioning time {@code AzureImageVmTypeCompatibilityValidator} is the second line of defence.
     *
     * @param availableVmTypes the VM types already fetched for the environment's region (credential, region, all architectures), or null to fetch them
     */
    public void validate(DetailedEnvironmentResponse environment, StackV4Request stackRequest, ImageV4Response imageV4Response,
            List<VmTypeResponse> availableVmTypes) {
        if (!CloudPlatform.AZURE.equalsIgnoreCase(environment.getCloudPlatform()) || imageV4Response == null) {
            return;
        }
        Optional<String> imageGeneration = AzureHyperVGenerationUtil.toGeneration(imageV4Response.getTags());
        if (imageGeneration.isEmpty() || CollectionUtils.isEmpty(stackRequest.getInstanceGroups())) {
            return;
        }
        String generation = imageGeneration.get();
        try {
            Map<String, VmTypeResponse> vmTypesByName = getVmTypesByName(environment, availableVmTypes);
            boolean fallbackEnabled = entitlementService.isFallbackInstanceTypeEnabled(environment.getAccountId());
            List<String> offenders = collectOffenders(stackRequest, generation, vmTypesByName, fallbackEnabled);
            if (!offenders.isEmpty()) {
                throw new BadRequestException(String.format("The selected image is Hyper-V generation %s, but the following instance types do not support "
                        + "it: %s. Choose a %s compatible instance type or an image of the matching generation.",
                        generation, String.join(", ", offenders), generation));
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Could not validate the Hyper-V generation of the instance types of environment {}. Proceeding without validation.",
                    environment.getCrn(), e);
        }
    }

    private Map<String, VmTypeResponse> getVmTypesByName(DetailedEnvironmentResponse environment, List<VmTypeResponse> availableVmTypes) {
        return Optional.ofNullable(availableVmTypes).orElseGet(() -> fetchAvailableVmTypes(environment))
                .stream()
                .collect(Collectors.toMap(VmTypeResponse::getValue, Function.identity(), (first, second) -> first));
    }

    private List<VmTypeResponse> fetchAvailableVmTypes(DetailedEnvironmentResponse environment) {
        String region = environment.getRegions().getNames().stream().findFirst().orElse(null);
        return sdxRecommendationService.getAvailableVmTypes(environment.getCredential().getCrn(), environment.getCloudPlatform(), region, null,
                Architecture.ALL_ARCHITECTURE);
    }

    private List<String> collectOffenders(StackV4Request stackRequest, String imageGeneration, Map<String, VmTypeResponse> vmTypesByName,
            boolean fallbackEnabled) {
        List<String> offenders = new ArrayList<>();
        for (InstanceGroupV4Request instanceGroup : stackRequest.getInstanceGroups()) {
            InstanceTemplateV4Base template = instanceGroup.getTemplate();
            if (template == null) {
                continue;
            }
            String instanceType = template.getInstanceType();
            if (StringUtils.isNotBlank(instanceType)) {
                addIfIncompatible(offenders, instanceType, instanceGroup.getName(), imageGeneration, vmTypesByName);
            }
            if (fallbackEnabled) {
                Optional.ofNullable(template.getFallbackInstanceTypes()).orElse(List.of()).stream()
                        .filter(StringUtils::isNotBlank)
                        .filter(fallback -> !fallback.equals(instanceType))
                        .distinct()
                        .forEach(fallback -> addIfIncompatible(offenders, fallback, instanceGroup.getName() + " fallback", imageGeneration,
                                vmTypesByName));
            }
        }
        return offenders;
    }

    private void addIfIncompatible(List<String> offenders, String instanceType, String label, String imageGeneration,
            Map<String, VmTypeResponse> vmTypesByName) {
        String supportedGenerations = getSupportedGenerations(vmTypesByName.get(instanceType));
        if (supportedGenerations != null && !supportedGenerations.contains(imageGeneration)) {
            offenders.add(String.format("%s (%s, supports %s)", instanceType, label, supportedGenerations));
        }
    }

    private String getSupportedGenerations(VmTypeResponse vmType) {
        return Optional.ofNullable(vmType)
                .map(VmTypeResponse::getVmTypeMetaJson)
                .map(VmTypeMetaJson::getProperties)
                .map(properties -> properties.get(VmTypeMeta.HYPER_V_GENERATIONS))
                .map(Object::toString)
                .orElse(null);
    }
}
