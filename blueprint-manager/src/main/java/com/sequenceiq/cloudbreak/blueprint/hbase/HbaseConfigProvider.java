package com.sequenceiq.cloudbreak.blueprint.hbase;

import java.util.Iterator;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class HbaseConfigProvider implements BlueprintComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HbaseConfigProvider.class);

    @Inject
    private StackInfoService stackInfoService;

    @Override
    public String customTextManipulation(BlueprintPreparationObject source, String blueprintText) {
        String processingBlueprint = blueprintText;
        try {
            JsonNode root = JsonUtil.readTree(processingBlueprint);
            ArrayNode hostGroupsNode = (ArrayNode) root.path("host_groups");
            Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
            while (hostGroups.hasNext()) {
                JsonNode hostGroupNode = hostGroups.next();
                ArrayNode componentsArray = (ArrayNode) hostGroupNode.path("components");
                Iterator<JsonNode> iterator = componentsArray.elements();
                boolean masterPresent = false;
                boolean clientPresent = false;
                while (iterator.hasNext()) {
                    String componentName = iterator.next().path("name").textValue();
                    if ("HBASE_MASTER".equals(componentName)) {
                        masterPresent = true;
                    } else if ("HBASE_CLIENT".equals(componentName)) {
                        clientPresent = true;
                    }
                }
                if (masterPresent && !clientPresent) {
                    ObjectNode arrayElementNode = componentsArray.addObject();
                    arrayElementNode.put("name", "HBASE_CLIENT");
                }
            }
            processingBlueprint = JsonUtil.writeValueAsString(root);
        } catch (Exception e) {
            LOGGER.warn("Cannot extend validation with HBASE_CLIENT", e);
        }
        return processingBlueprint;
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return !stackInfoService.hdfCluster(source.getStackRepoDetails());
    }
}
