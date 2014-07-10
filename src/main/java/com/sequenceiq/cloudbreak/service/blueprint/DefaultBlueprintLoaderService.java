package com.sequenceiq.cloudbreak.service.blueprint;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class DefaultBlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintLoaderService.class);

    @Value("#{'${blueprint.defaults}'.split(',')}")
    private List<String> blueprintArray;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private JsonHelper jsonHelper;

    public Set<Blueprint> loadBlueprints(User user) {
        Set<Blueprint> blueprints = new HashSet<>();
        for (String blueprintName : blueprintArray) {
            try {
                BlueprintJson blueprintJson = new BlueprintJson();
                blueprintJson.setBlueprintName(blueprintName);
                blueprintJson.setName(blueprintName);
                blueprintJson.setDescription(blueprintName);
                blueprintJson.setAmbariBlueprint(
                        jsonHelper.createJsonFromString(FileReaderUtils.readFileFromClasspath(String.format("blueprints/%s.bp", blueprintName)))
                );

                Blueprint bp = blueprintConverter.convert(blueprintJson);
                bp.setUser(user);
                blueprints.add(bp);
            } catch (IOException e) {
                LOGGER.error(blueprintName + " blueprint is not available.", e);
            }
        }
        return blueprints;
    }
}
