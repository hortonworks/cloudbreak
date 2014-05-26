package com.sequenceiq.provisioning.converter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.domain.Blueprint;

@Component
public class BlueprintConverter  extends AbstractConverter<BlueprintJson, Blueprint> {

    @Override
    public BlueprintJson convert(Blueprint entity) {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setName(entity.getName());
        JsonNode node = new TextNode(entity.getBlueprintText());
        blueprintJson.setAmbariBlueprint(node);
        return blueprintJson;
    }

    @Override
    public Blueprint convert(BlueprintJson json) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(json.getName());
        blueprint.setBlueprintText(json.getAmbariBlueprint());
        return blueprint;
    }
}
