package com.sequenceiq.cloudbreak.converter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class TemplateRequestToTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateRequest, Template> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateRequestToTemplateConverter.class);

    @Inject
    private TopologyService topologyService;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public Template convert(TemplateRequest source) {
        Template template = new Template();

        if (Strings.isNullOrEmpty(source.getName())) {
            template.setName(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE));
        } else {
            template.setName(source.getName());
        }
        template.setDescription(source.getDescription());
        template.setStatus(ResourceStatus.USER_MANAGED);
        template = convertVolume(source, template);
        template.setCloudPlatform(source.getCloudPlatform());
        template.setRootVolumeSize(source.getRootVolumeSize());
        template.setInstanceType(source.getInstanceType() == null ? "" : source.getInstanceType());
        Map<String, Object> parameters = source.getParameters() == null ? Maps.newHashMap() : source.getParameters();
        CustomInstanceType customInstanceType = source.getCustomInstanceType();
        if (customInstanceType != null) {
            parameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, customInstanceType.getMemory());
            parameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, customInstanceType.getCpus());
        }

        Optional.ofNullable(parameters).map(toJson()).ifPresent(template::setAttributes);
        Optional.ofNullable(source.getSecretParameters()).map(toJson()).map(Json::getValue).ifPresent(template::setSecretAttributes);

        if (source.getTopologyId() != null) {
            template.setTopology(topologyService.get(source.getTopologyId()));
        }
        return template;
    }

    private Function<Map<String, Object>, Json> toJson() {
        return value -> {
            try {
                return new Json(value);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to parse template parameters as JSON.", e);
                throw new BadRequestException("Invalid template parameter format, valid JSON expected.");
            }
        };
    }

    private Template convertVolume(TemplateRequest source, Template template) {
        String volumeType = source.getVolumeType();
        template.setVolumeType(volumeType == null ? "HDD" : volumeType);
        Integer volumeCount = source.getVolumeCount();
        template.setVolumeCount(volumeCount == null ? Integer.valueOf(0) : volumeCount);
        Integer volumeSize = source.getVolumeSize();
        template.setVolumeSize(volumeSize == null ? Integer.valueOf(0) : volumeSize);
        return template;
    }
}
