package com.sequenceiq.provisioning.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.domain.Blueprint;

@Component
public class BlueprintConverter extends AbstractConverter<BlueprintJson, Blueprint> {

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
        if (json.getUrl() != null) {
            try {
                blueprint.setBlueprintText(readUrl(json.getAmbariBlueprint()));
            } catch (IOException e) {
                throw new BadRequestException("Cannot download ambari blueprint from: " + json.getUrl());
            }
        } else {
            blueprint.setBlueprintText(json.getAmbariBlueprint());
        }
        return blueprint;
    }

    private String readUrl(String url) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String str;
        StringBuffer sb = new StringBuffer();
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();
        return sb.toString();
    }
}
