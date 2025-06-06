package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;

@Component
public class InstanceTemplateV4RequestToTemplateConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTemplateV4RequestToTemplateConverter.class);

    @Inject
    private ResourceNameGenerator resourceNameGenerator;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    public Template convert(InstanceTemplateV4Request source, boolean gatewayType) {
        Template template = new Template();
        template.setName(resourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.USER_MANAGED);
        template.setCloudPlatform(source.getCloudPlatform().name());
        template.setVolumeTemplates(Sets.newHashSet());
        setVolumesProperty(source.getAttachedVolumes(), Optional.ofNullable(source.getRootVolume()), template, gatewayType);
        template.setInstanceType(source.getInstanceType() == null ? "" : source.getInstanceType());
        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        Optional.ofNullable(parameters).map(toJson()).ifPresent(template::setAttributes);
        Map<String, Object> secretParameters = providerParameterCalculator.get(source).asSecretMap();
        Optional.ofNullable(secretParameters).map(toJson()).map(Json::getValue).ifPresent(template::setSecretAttributes);
        template.setTemporaryStorage(source.getTemporaryStorage() != null
                ? source.getTemporaryStorage()
                : TemporaryStorage.ATTACHED_VOLUMES);
        return template;
    }

    private Function<Map<String, Object>, Json> toJson() {
        return value -> {
            try {
                return new Json(value);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Failed to parse template parameters as JSON.", e);
                throw new BadRequestException("Invalid template parameter format, valid JSON expected.");
            }
        };
    }

    private void setVolumesProperty(Set<VolumeV4Request> attachedVolumes, Optional<RootVolumeV4Request> rootVolume, Template template, boolean gatewayType) {
        if (!attachedVolumes.isEmpty()) {
            attachedVolumes.stream().forEach(v -> {
                String volumeType = v.getType();
                Integer volumeCount = v.getCount();
                Integer volumeSize = v.getSize();
                VolumeTemplate volumeTemplate = new VolumeTemplate();
                volumeTemplate.setVolumeCount(volumeCount == null ? Integer.valueOf(0) : volumeCount);
                volumeTemplate.setVolumeType(volumeType == null ? "HDD" : volumeType);
                volumeTemplate.setVolumeSize(volumeSize == null ? Integer.valueOf(0) : volumeSize);
                volumeTemplate.setUsageType(VolumeUsageType.GENERAL);
                volumeTemplate.setTemplate(template);
                template.getVolumeTemplates().add(volumeTemplate);
            });
        } else {
            VolumeTemplate volumeTemplate = new VolumeTemplate();
            volumeTemplate.setVolumeCount(0);
            volumeTemplate.setVolumeType("HDD");
            volumeTemplate.setVolumeSize(0);
            volumeTemplate.setTemplate(template);
            template.getVolumeTemplates().add(volumeTemplate);
        }
        template.setRootVolumeSize(rootVolume.map(RootVolumeV4Request::getSize).isPresent()
                ? rootVolume.get().getSize()
                : defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(template.getCloudPlatform(), gatewayType));
    }
}