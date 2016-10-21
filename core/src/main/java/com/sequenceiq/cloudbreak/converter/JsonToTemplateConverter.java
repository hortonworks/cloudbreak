package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@Component
public class JsonToTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateRequest, Template> {
    @Inject
    private TopologyService topologyService;

    @Override
    public Template convert(TemplateRequest source) {
        Template template = new Template();
        template.setName(source.getName());
        template.setDescription(source.getDescription());
        template.setStatus(ResourceStatus.USER_MANAGED);
        template.setVolumeCount(source.getVolumeCount());
        template.setVolumeSize(source.getVolumeSize());
        template.setCloudPlatform(source.getCloudPlatform());
        template.setInstanceType(source.getInstanceType());
        String volumeType = source.getVolumeType();
        template.setVolumeType(volumeType == null ? "HDD" : volumeType);
        Map<String, Object> parameters = source.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            try {
                template.setAttributes(new Json(parameters));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        if (source.getTopologyId() != null) {
            template.setTopology(topologyService.get(source.getTopologyId()));
        }
        return template;
    }
}
