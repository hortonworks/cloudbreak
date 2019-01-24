package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class InstanceTemplateV4RequestToTemplateConverter extends AbstractConversionServiceAwareConverter<InstanceTemplateV4Request, Template> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTemplateV4RequestToTemplateConverter.class);

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public Template convert(InstanceTemplateV4Request source) {
        Template template = new Template();

        template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        template.setStatus(ResourceStatus.USER_MANAGED);
        setVolumesProperty(source, template);
        template.setCloudPlatform(source.getCloudPlatform().name());
        template.setInstanceType(source.getInstanceType() == null ? "" : source.getInstanceType());
        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        Optional.ofNullable(parameters).map(toJson()).ifPresent(template::setAttributes);
        Map<String, Object> secretParameters = providerParameterCalculator.get(source).asSecretMap();
        Optional.ofNullable(secretParameters).map(toJson()).map(Json::getValue).ifPresent(template::setSecretAttributes);
        return template;
    }

    private Function<Map<String, Object>, Json> toJson() {
        return value -> {
            try {
                return new Json(value);
            } catch (JsonProcessingException e) {
                LOGGER.info("Failed to parse template parameters as JSON.", e);
                throw new BadRequestException("Invalid template parameter format, valid JSON expected.");
            }
        };
    }

    private void setVolumesProperty(InstanceTemplateV4Request source, Template template) {
        source.getAttachedVolumes().stream().findFirst().ifPresent(v -> {
            String volumeType = v.getType();
            template.setVolumeType(volumeType == null ? "HDD" : volumeType);
            Integer volumeCount = v.getCount();
            template.setVolumeCount(volumeCount == null ? Integer.valueOf(0) : volumeCount);
            Integer volumeSize = v.getSize();
            template.setVolumeSize(volumeSize == null ? Integer.valueOf(0) : volumeSize);
        });
        template.setRootVolumeSize(source.getRootVolume().getSize());
    }
}
