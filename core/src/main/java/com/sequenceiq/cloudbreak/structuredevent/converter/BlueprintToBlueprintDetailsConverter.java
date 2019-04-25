package com.sequenceiq.cloudbreak.structuredevent.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;

@Component
public class BlueprintToBlueprintDetailsConverter extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintDetails> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintDetailsConverter.class);

    @Override
    public BlueprintDetails convert(Blueprint source) {
        BlueprintDetails blueprintDetails = new BlueprintDetails();
        blueprintDetails.setId(source.getId());
        blueprintDetails.setName(source.getName());
        blueprintDetails.setDescription(source.getDescription());
        blueprintDetails.setBlueprintName(source.getStackName());
        blueprintDetails.setBlueprintJson(source.getBlueprintText());
        return blueprintDetails;
    }
}
