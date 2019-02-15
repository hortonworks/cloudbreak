package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToBlueprintRequestConverter
        extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintRequestConverter.class);

    @Override
    public BlueprintRequest convert(Blueprint source) {
        BlueprintRequest blueprintRequest = new BlueprintRequest();
        blueprintRequest.setName("");
        blueprintRequest.setDescription(source.getDescription());
        blueprintRequest.setAmbariBlueprint(source.getBlueprintText());
        return blueprintRequest;
    }

}
