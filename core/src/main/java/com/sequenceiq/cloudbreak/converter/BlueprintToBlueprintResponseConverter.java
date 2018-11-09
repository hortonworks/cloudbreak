package com.sequenceiq.cloudbreak.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToBlueprintResponseConverter extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToBlueprintResponseConverter.class);

    @Override
    public BlueprintResponse convert(Blueprint entity) {
        BlueprintResponse blueprintJson = new BlueprintResponse();
        blueprintJson.setId(entity.getId());
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setTags(entity.getTags().getMap());
        blueprintJson.setAmbariBlueprint(entity.getBlueprintText().getRaw());
        return blueprintJson;
    }

}
