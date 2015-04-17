package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.BlueprintResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintToJsonConverter extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToJsonConverter.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public BlueprintResponse convert(Blueprint entity) {
        BlueprintResponse blueprintJson = new BlueprintResponse();
        blueprintJson.setId(String.valueOf(entity.getId()));
        blueprintJson.setBlueprintName(entity.getBlueprintName());
        blueprintJson.setName(entity.getName());
        blueprintJson.setPublicInAccount(entity.isPublicInAccount());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        blueprintJson.setHostGroupCount(entity.getHostGroupCount());
        try {
            blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(entity.getBlueprintText()));
        } catch (Exception e) {
            LOGGER.error("Blueprint cannot be converted to JSON.", e);
            blueprintJson.setAmbariBlueprint(new TextNode(e.getMessage()));
        }
        return blueprintJson;
    }
}
