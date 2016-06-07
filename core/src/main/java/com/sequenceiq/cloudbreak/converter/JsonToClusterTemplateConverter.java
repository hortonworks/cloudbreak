package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class JsonToClusterTemplateConverter extends AbstractConversionServiceAwareConverter<ClusterTemplateRequest, ClusterTemplate> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToClusterTemplateConverter.class);

    @Inject
    private JsonHelper jsonHelper;

    @Override
    public ClusterTemplate convert(ClusterTemplateRequest json) {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setName(json.getName());
        try {
            clusterTemplate.setTemplate(new Json(json.getTemplate()));
        } catch (JsonProcessingException e) {
            LOGGER.error("Cloudtemplate cannot be converted to JSON: " + json.getTemplate(), e);
            throw new BadRequestException("Cloudtemplate cannot be converted to JSON", e);
        }
        clusterTemplate.setType(json.getType());
        return clusterTemplate;
    }
}
