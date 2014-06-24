package com.sequenceiq.cloudbreak.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintConverter extends AbstractConverter<BlueprintJson, Blueprint> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintConverter.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public BlueprintJson convert(Blueprint entity) {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setId(String.valueOf(entity.getId()));
        blueprintJson.setBlueprintName(entity.getBlueprintName());
        blueprintJson.setName(entity.getName());
        blueprintJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        try {
            blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(entity.getBlueprintText()));
        } catch (Exception e) {
            LOGGER.error("Blueprint cannot be converted to JSON.", e);
            blueprintJson.setAmbariBlueprint(new TextNode(e.getMessage()));
        }
        return blueprintJson;
    }

    @Override
    public Blueprint convert(BlueprintJson json) {
        Blueprint blueprint = new Blueprint();
        if (json.getUrl() != null) {
            try {
                String urlText = readUrl(json.getUrl());
                jsonHelper.createJsonFromString(urlText);
                blueprint.setBlueprintText(urlText);
            } catch (IOException e) {
                throw new BadRequestException("Cannot download ambari blueprint from: " + json.getUrl(), e);
            }
        } else {
            blueprint.setBlueprintText(json.getAmbariBlueprint());
        }
        Pattern p = Pattern.compile("\"blueprint_name\"(.*):(.*)\"(.*),");
        Matcher m = p.matcher(blueprint.getBlueprintText());
        blueprint.setName(json.getName());
        blueprint.setDescription(json.getDescription());
        if (m.find()) {
            blueprint.setBlueprintName(m.group()
                    .replaceAll(",(.*)\"(.*)stack_name(.*)", "")
                    .replaceAll("\"blueprint_name\"(.*):", "")
                    .trim()
                    .replaceAll("\"", ""));
        } else {
            throw new BadRequestException("Cannot parse ambari blueprint");
        }
        return blueprint;
    }

    public Set<BlueprintJson> convertAllToIdList(Collection<Blueprint> entityList) {
        return FluentIterable.from(entityList).transform(new Function<Blueprint, BlueprintJson>() {
            @Override
            public BlueprintJson apply(Blueprint e) {
                BlueprintJson blueprintJson = new BlueprintJson();
                blueprintJson.setName(e.getName());
                blueprintJson.setBlueprintName(e.getBlueprintName());
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
