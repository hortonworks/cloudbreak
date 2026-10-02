package com.sequenceiq.freeipa.service.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.AzureHyperVGenerationUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.stack.FreeIpaRecommendationService;

@Service
public class FreeIpaHyperVGenerationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaHyperVGenerationValidator.class);

    @Inject
    private FreeIpaRecommendationService freeIpaRecommendationService;

    @Inject
    private EntitlementService entitlementService;

    /**
     * Rejects an Azure image / instance type Hyper-V generation mismatch at request time, so the user gets a 400 instead of a provisioning failure minutes
     * later. When instance type fallback is enabled for the account, the fallback instance types are checked too, as the deployment may switch to any
     * of them after a capacity error. Deliberately fails open: an image without the generation tag, or an instance type whose supported generations
     * Azure did not report, is not blocked here - the provisioning time {@code AzureImageVmTypeCompatibilityValidator} is the second line of defence.
     *
     * @param availableVmTypes the VM types already fetched for the stack's region, or empty to fetch them
     */
    public void validate(Stack stack, Credential credential, Image targetImage, Set<VmType> availableVmTypes) {
        if (!CloudPlatform.AZURE.equalsIgnoreCase(stack.getCloudPlatform()) || targetImage == null) {
            return;
        }
        Optional<String> imageGeneration = AzureHyperVGenerationUtil.toGeneration(targetImage.getTags());
        if (imageGeneration.isEmpty() || CollectionUtils.isEmpty(stack.getInstanceGroups())) {
            return;
        }
        String generation = imageGeneration.get();
        try {
            Map<String, VmType> vmTypesByName = getVmTypesByName(stack, credential, availableVmTypes);
            boolean fallbackEnabled = entitlementService.isFallbackInstanceTypeEnabled(stack.getAccountId());
            List<String> offenders = collectOffenders(stack, generation, vmTypesByName, fallbackEnabled);
            if (!offenders.isEmpty()) {
                throw new BadRequestException(String.format("The selected image is Hyper-V generation %s, but the following instance types do not support "
                        + "it: %s. Choose a %s compatible instance type or an image of the matching generation.",
                        generation, String.join(", ", offenders), generation));
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Could not validate the Hyper-V generation of the instance types of stack {}. Proceeding without validation.", stack.getName(), e);
        }
    }

    private Map<String, VmType> getVmTypesByName(Stack stack, Credential credential, Set<VmType> availableVmTypes) {
        Set<VmType> vmTypes = CollectionUtils.isEmpty(availableVmTypes)
                ? freeIpaRecommendationService.getAvailableVmTypes(stack, credential)
                : availableVmTypes;
        return vmTypes.stream()
                .collect(Collectors.toMap(VmType::getValue, Function.identity(), (first, second) -> first));
    }

    private List<String> collectOffenders(Stack stack, String imageGeneration, Map<String, VmType> vmTypesByName, boolean fallbackEnabled) {
        List<String> offenders = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Template template = instanceGroup.getTemplate();
            if (template == null) {
                continue;
            }
            String instanceType = template.getInstanceType();
            if (StringUtils.isNotBlank(instanceType)) {
                addIfIncompatible(offenders, instanceType, instanceGroup.getGroupName(), imageGeneration, vmTypesByName);
            }
            if (fallbackEnabled) {
                template.getFallbackInstanceTypesAsList().stream()
                        .filter(StringUtils::isNotBlank)
                        .filter(fallback -> !fallback.equals(instanceType))
                        .distinct()
                        .forEach(fallback -> addIfIncompatible(offenders, fallback, instanceGroup.getGroupName() + " fallback", imageGeneration,
                                vmTypesByName));
            }
        }
        return offenders;
    }

    private void addIfIncompatible(List<String> offenders, String instanceType, String label, String imageGeneration, Map<String, VmType> vmTypesByName) {
        getVmTypeMeta(vmTypesByName.get(instanceType))
                .filter(vmTypeMeta -> !vmTypeMeta.isHyperVGenerationSupported(imageGeneration))
                .ifPresent(vmTypeMeta -> offenders.add(String.format("%s (%s, supports %s)", instanceType, label,
                        vmTypeMeta.getHyperVGenerations().orElse("unknown"))));
    }

    private Optional<VmTypeMeta> getVmTypeMeta(VmType vmType) {
        return Optional.ofNullable(vmType)
                .filter(VmType::isMetaSet)
                .map(VmType::getMetaData);
    }
}
