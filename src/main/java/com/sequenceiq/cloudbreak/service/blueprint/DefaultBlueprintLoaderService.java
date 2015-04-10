package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class DefaultBlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintLoaderService.class);

    @Value("#{'${cb.blueprint.defaults:multi-node-hdfs-yarn,hdp-multinode-default}'.split(',')}")
    private List<String> blueprintArray;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private JsonHelper jsonHelper;

    public Set<Blueprint> loadBlueprints(CbUser user) {
        Set<Blueprint> blueprints = new HashSet<>();
        for (String blueprintName : blueprintArray) {
            LOGGER.info("Adding default blueprint '{}' for user '{}'", blueprintName, user.getUsername());
            try {
                BlueprintJson blueprintJson = new BlueprintJson();
                blueprintJson.setBlueprintName(blueprintName);
                blueprintJson.setName(blueprintName);
                blueprintJson.setDescription(blueprintName);
                blueprintJson.setAmbariBlueprint(
                        jsonHelper.createJsonFromString(FileReaderUtils.readFileFromClasspath(String.format("blueprints/%s.bp", blueprintName))));
                Blueprint bp = conversionService.convert(blueprintJson, Blueprint.class);
                bp.setOwner(user.getUserId());
                bp.setAccount(user.getAccount());
                bp.setPublicInAccount(true);
                blueprints.add(bp);
            } catch (Exception e) {
                LOGGER.error("Blueprint is not available for '{}' user.", e, user);
            }
        }
        return blueprints;
    }
}
