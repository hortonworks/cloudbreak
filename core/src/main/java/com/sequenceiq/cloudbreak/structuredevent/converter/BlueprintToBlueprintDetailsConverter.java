package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintToBlueprintDetailsConverter extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintDetails> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintDetailsConverter.class);

    @Override
    public BlueprintDetails convert(Blueprint source) {
        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setId(source.getId());
        blueprintDetails.setName(source.getName());
        blueprintDetails.setDescription(source.getDescription());
        blueprintDetails.setBlueprintName(source.getBlueprintName());
        try {
            blueprintDetails.setBlueprintJson(JsonUtil.readTree(source.getBlueprintText()));
        } catch (IOException e) {
            LOGGER.warn("Cannot parse bluepirnt text to json during structured event creation: {}", e.getMessage());
        }
        return blueprintDetails;
    }
}
