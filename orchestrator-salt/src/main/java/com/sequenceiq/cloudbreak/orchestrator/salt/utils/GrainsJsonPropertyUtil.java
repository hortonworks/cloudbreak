package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IteratorUtils;

import com.fasterxml.jackson.databind.JsonNode;

public class GrainsJsonPropertyUtil {

    private GrainsJsonPropertyUtil() {
    }

    public static Set<String> getPropertySet(JsonNode grainsProperty) {
        return Optional.ofNullable(grainsProperty)
                .map(JsonNode::elements)
                .map(IteratorUtils::toList)
                .map(jsonNodeList -> (Set<String>) jsonNodeList.stream()
                        .map(item -> ((JsonNode) item).asText())
                        .collect(Collectors.toSet())
                ).orElse(Collections.emptySet());
    }

}
