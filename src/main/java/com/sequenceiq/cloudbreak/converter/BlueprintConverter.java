package com.sequenceiq.cloudbreak.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.InvalidBlueprintException;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;

@Component
public class BlueprintConverter extends AbstractConverter<BlueprintJson, Blueprint> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintConverter.class);

    @Autowired
    private JsonHelper jsonHelper;

    @Override
    public BlueprintJson convert(Blueprint entity) {
        CbLoggerFactory.buildMdcContext(entity);
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setId(String.valueOf(entity.getId()));
        blueprintJson.setBlueprintName(entity.getBlueprintName());
        blueprintJson.setName(entity.getName());
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

    @Override
    public Blueprint convert(BlueprintJson json) {
        Blueprint blueprint = new Blueprint();
        if (json.getUrl() != null && !json.getUrl().isEmpty()) {
            String sourceUrl = json.getUrl().trim();
            try {
                String urlText = readUrl(sourceUrl);
                jsonHelper.createJsonFromString(urlText);
                blueprint.setBlueprintText(urlText);
            } catch (Exception e) {
                throw new BadRequestException("Cannot download ambari blueprint from: " + sourceUrl, e);
            }
        } else {
            blueprint.setBlueprintText(json.getAmbariBlueprint());
        }

        validateBlueprint(blueprint.getBlueprintText());

        blueprint.setName(json.getName());
        blueprint.setDescription(json.getDescription());
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(blueprint.getBlueprintText());
            int hostGroupCount = 0;
            blueprint.setBlueprintName(root.get("Blueprints").get("blueprint_name").asText());
            Iterator<JsonNode> hostGroups = root.get("host_groups").elements();
            while (hostGroups.hasNext()) {
                hostGroups.next();
                hostGroupCount++;
            }
            blueprint.setHostGroupCount(hostGroupCount);
        } catch (IOException e) {
            throw new BadRequestException("Invalid Blueprint: Failed to parse JSON.", e);
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

    private void validateBlueprint(String blueprintText) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(blueprintText);
            if (root.path("Blueprints").isMissingNode()) {
                throw new BadRequestException("Invalid blueprint: 'Blueprints' node is missing from JSON.");
            }
            if (root.path("Blueprints").path("blueprint_name").isMissingNode()) {
                throw new BadRequestException("Invalid blueprint: 'blueprint_name' under 'Blueprints' is missing from JSON.");
            }
            if (root.path("host_groups").isMissingNode() || !root.path("host_groups").isArray()) {
                throw new BadRequestException("Invalid blueprint: 'host_groups' node is missing from JSON or is not an array.");
            }
            Iterator<JsonNode> hostGroupsIterator = root.path("host_groups").elements();
            while (hostGroupsIterator.hasNext()) {
                JsonNode hostGroup = hostGroupsIterator.next();
                if (hostGroup.path("name").isMissingNode()) {
                    throw new BadRequestException("Invalid blueprint: every 'host_group' must have a 'name' attribute.");
                }
            }
            new AmbariClient().validateBlueprint(blueprintText);
        } catch (InvalidBlueprintException e) {
            throw new BadRequestException("Invalid Blueprint: At least one host group with 'slave_' prefix is required in the blueprint.", e);
        } catch (IOException e) {
            throw new BadRequestException("Invalid Blueprint: Failed to parse JSON.", e);
        }
    }
}
