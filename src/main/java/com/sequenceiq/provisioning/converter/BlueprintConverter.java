package com.sequenceiq.provisioning.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.provisioning.controller.BadRequestException;
import com.sequenceiq.provisioning.controller.json.BlueprintJson;
import com.sequenceiq.provisioning.controller.json.JsonHelper;
import com.sequenceiq.provisioning.domain.Blueprint;

@Component
public class BlueprintConverter extends AbstractConverter<BlueprintJson, Blueprint> {

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public BlueprintJson convert(Blueprint entity) {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setId(String.valueOf(entity.getId()));
        blueprintJson.setName(entity.getName());
        try {
            blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(entity.getBlueprintText()));
        } catch (Exception ex) {
            blueprintJson.setAmbariBlueprint(new TextNode(ex.getMessage()));
        }
        return blueprintJson;
    }

    @Override
    public Blueprint convert(BlueprintJson json) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(json.getName());
        if (json.getUrl() != null) {
            try {
                String urlText = readUrl(json.getUrl());
                jsonHelper.createJsonFromString(urlText);
                blueprint.setBlueprintText(urlText);
            } catch (IOException e) {
                throw new BadRequestException("Cannot download ambari blueprint from: " + json.getUrl());
            }
        } else {
            blueprint.setBlueprintText(json.getAmbariBlueprint());
        }
        return blueprint;
    }

    public Set<BlueprintJson> convertAllToIdList(Collection<Blueprint> entityList) {
        return FluentIterable.from(entityList).transform(new Function<Blueprint, BlueprintJson>() {
            @Override
            public BlueprintJson apply(Blueprint e) {
                BlueprintJson blueprintJson = new BlueprintJson();
                blueprintJson.setName(e.getName());
                blueprintJson.setId(String.valueOf(e.getId()));
                return convert(e);
            }
        }).toSet();
    }

    private String readUrl(String url) throws IOException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
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
