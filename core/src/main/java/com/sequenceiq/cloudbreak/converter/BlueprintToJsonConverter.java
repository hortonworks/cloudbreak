package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.json.Json;

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
        try {
            blueprintJson.setInputs(convertInputParameters(entity.getInputParameters()));
        } catch (IOException e) {
            LOGGER.error(String.format("Blueprint's (%s, id:%s) input parameters could not be converted to JSON.", entity.getName(), entity.getId()), e);
        }
        try {
            blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(entity.getBlueprintText()));
        } catch (Exception e) {
            LOGGER.error("Blueprint cannot be converted to JSON.", e);
            blueprintJson.setAmbariBlueprint(new TextNode(e.getMessage()));
        }
        return blueprintJson;
    }

    private Set<BlueprintParameterJson> convertInputParameters(Json inputParameters) throws IOException {
        Set<BlueprintParameterJson> result = new HashSet<>();
        if (inputParameters != null && StringUtils.isNoneEmpty(inputParameters.getValue())) {
            BlueprintInputParameters inputParametersObj = inputParameters.get(BlueprintInputParameters.class);
            List<BlueprintParameter> parameters = inputParametersObj.getParameters();
            for (BlueprintParameter record : parameters) {
                BlueprintParameterJson json = new BlueprintParameterJson();
                json.setDescription(record.getDescription());
                json.setName(record.getName());
                json.setReferenceConfiguration(record.getReferenceConfiguration());
                result.add(json);
            }
        }
        return result;
    }
}
