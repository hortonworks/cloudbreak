package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToBlueprintV4RequestConverter
        extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintV4RequestConverter.class);

    @Override
    public BlueprintV4Request convert(Blueprint source) {
        BlueprintV4Request blueprintV4Request = new BlueprintV4Request();
        blueprintV4Request.setName(source.getName());
        blueprintV4Request.setDescription(source.getDescription());
        blueprintV4Request.setBlueprint(source.getBlueprintText());
        blueprintV4Request.setDescription(source.getDescription());
        return blueprintV4Request;
    }

}
