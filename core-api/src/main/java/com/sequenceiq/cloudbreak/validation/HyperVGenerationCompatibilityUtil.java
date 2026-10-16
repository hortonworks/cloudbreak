package com.sequenceiq.cloudbreak.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

public class HyperVGenerationCompatibilityUtil {

    private HyperVGenerationCompatibilityUtil() {
    }

    /**
     * Throws a {@link BadRequestException} listing every instance type of the request (and its fallback instance types, when enabled) that does not
     * support the given Hyper-V generation. An instance type for which the lookup returns empty is not blocked, as its supported generations are unknown.
     *
     * @param supportedGenerationsByInstanceType the comma separated supported Hyper-V generations of an instance type, e.g. "V1,V2"
     */
    public static void validate(StackV4Request stackRequest, String imageGeneration,
            Function<String, Optional<String>> supportedGenerationsByInstanceType, boolean fallbackEnabled) {
        List<String> offenders = collectOffenders(stackRequest, imageGeneration, supportedGenerationsByInstanceType, fallbackEnabled);
        if (!offenders.isEmpty()) {
            throw new BadRequestException(String.format("The selected image is Hyper-V generation %s, but the following instance types do not support "
                            + "it: %s. Choose a %s compatible instance type or an image of the matching generation.",
                    imageGeneration, String.join(", ", offenders), imageGeneration));
        }
    }

    private static List<String> collectOffenders(StackV4Request stackRequest, String imageGeneration,
            Function<String, Optional<String>> supportedGenerationsByInstanceType, boolean fallbackEnabled) {
        List<String> offenders = new ArrayList<>();
        for (InstanceGroupV4Request instanceGroup : Optional.ofNullable(stackRequest.getInstanceGroups()).orElse(List.of())) {
            InstanceTemplateV4Base template = instanceGroup.getTemplate();
            if (template == null) {
                continue;
            }
            String instanceType = template.getInstanceType();
            if (StringUtils.isNotBlank(instanceType)) {
                addIfIncompatible(offenders, instanceType, instanceGroup.getName(), imageGeneration, supportedGenerationsByInstanceType);
            }
            if (fallbackEnabled) {
                Optional.ofNullable(template.getFallbackInstanceTypes()).orElse(List.of()).stream()
                        .filter(StringUtils::isNotBlank)
                        .filter(fallback -> !fallback.equals(instanceType))
                        .distinct()
                        .forEach(fallback -> addIfIncompatible(offenders, fallback, instanceGroup.getName() + " fallback", imageGeneration,
                                supportedGenerationsByInstanceType));
            }
        }
        return offenders;
    }

    private static void addIfIncompatible(List<String> offenders, String instanceType, String label, String imageGeneration,
            Function<String, Optional<String>> supportedGenerationsByInstanceType) {
        supportedGenerationsByInstanceType.apply(instanceType)
                .filter(supportedGenerations -> !supportedGenerations.contains(imageGeneration))
                .ifPresent(supportedGenerations -> offenders.add(String.format("%s (%s, supports %s)", instanceType, label, supportedGenerations)));
    }
}
