package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import java.util.Map;

import org.springframework.beans.factory.FactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;

public class StackServiceComponentDescriptorMapFactory implements FactoryBean<StackServiceComponentDescriptors> {
    private String stackServiceComponentsJson;
    private Map<String, Integer> maxCardinalityReps;
    private ObjectMapper objectMapper;

    public StackServiceComponentDescriptorMapFactory(String stackServiceComponentsJson, Map<String, Integer> maxCardinalityReps, ObjectMapper objectMapper) {
        this.stackServiceComponentsJson = stackServiceComponentsJson;
        this.maxCardinalityReps = maxCardinalityReps;
        this.objectMapper = objectMapper;
    }

    @Override
    public StackServiceComponentDescriptors getObject() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap = Maps.newHashMap();
        JsonNode rootNode = objectMapper.readTree(stackServiceComponentsJson);
        JsonNode itemsNode = rootNode.get("items");
        for (JsonNode itemNode : itemsNode) {
            JsonNode componentsNode = itemNode.get("components");
            for (JsonNode componentNode : componentsNode) {
                JsonNode stackServiceComponentsNode = componentNode.get("StackServiceComponents");
                StackServiceComponentDescriptor componentDesc = createComponentDesc(stackServiceComponentsNode);
                stackServiceComponentDescriptorMap.put(componentDesc.getName(), componentDesc);
            }
        }
        return new StackServiceComponentDescriptors(stackServiceComponentDescriptorMap);
    }

    @Override
    public Class<?> getObjectType() {
        return Map.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private StackServiceComponentDescriptor createComponentDesc(JsonNode stackServiceComponentNode) {
        String componentName = stackServiceComponentNode.get("component_name").asText();
        String componentCategory = stackServiceComponentNode.get("component_category").asText();
        int maxCardinality = parseMaxCardinality(stackServiceComponentNode.get("cardinality").asText());
        return new StackServiceComponentDescriptor(componentName, componentCategory, maxCardinality);
    }

    private int parseMaxCardinality(String cardinalityString) {
        Integer maxCardinality = maxCardinalityReps.get(cardinalityString);
        return maxCardinality == null ? Integer.MAX_VALUE : maxCardinality;
    }
}
