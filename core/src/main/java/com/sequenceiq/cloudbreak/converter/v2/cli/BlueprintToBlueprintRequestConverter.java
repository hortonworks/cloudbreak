package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToBlueprintRequestConverter
        extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintRequestConverter.class);

    @Override
    public BlueprintV4Request convert(Blueprint source) {
        BlueprintV4Request blueprintV4Request = new BlueprintV4Request();
        blueprintV4Request.setName("");
        blueprintV4Request.setDescription(source.getDescription());
        blueprintV4Request.setAmbariBlueprint(source.getBlueprintText());
        return blueprintV4Request;
    }

}
