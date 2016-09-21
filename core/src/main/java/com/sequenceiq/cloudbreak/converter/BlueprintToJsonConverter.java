package com.sequenceiq.cloudbreak.converter;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;

@Component
public class BlueprintToJsonConverter extends AbstractConversionServiceAwareConverter<Blueprint, BlueprintResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintToJsonConverter.class);

    @Inject
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
        blueprintJson.setStatus(entity.getStatus());
        blueprintJson.setInputs(convertNodes(entity.getInputs()));
        try {
            blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(entity.getBlueprintText()));
        } catch (Exception e) {
            LOGGER.error("Blueprint cannot be converted to JSON.", e);
            blueprintJson.setAmbariBlueprint(new TextNode(e.getMessage()));
        }
        return blueprintJson;
    }

    private Set<BlueprintParameterJson> convertNodes(Set<BlueprintParameter> records) {
        Set<BlueprintParameterJson> result = new HashSet<>();
        for (BlueprintParameter record : records) {
            BlueprintParameterJson json = new BlueprintParameterJson();
            json.setDescription(record.getDescription());
            json.setName(record.getName());
            json.setReferenceConfiguration(record.getReferenceConfiguration());
            result.add(json);
        }
        return result;
    }
}
