package com.sequenceiq.cloudbreak.template.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class ModelConverterUtils {

    private static final String SEGMENT_CHARACTER = ".";

    private static final String ESCAPED_SEGMENT_CHARACTER = "\\.";

    private ModelConverterUtils() {
    }

    public static Map<String, Object> convert(Map<String, Object> input) {
        Map<String, Object> retMap = new HashMap<>();
        if (input != null) {
            retMap = toMap(JsonUtil.createJsonTree(input));
        }
        return retMap;
    }

    private static Map<String, Object> toMap(ObjectNode objectNode) {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = objectNode.fieldNames();
        while (keysItr.hasNext()) {
            String key = keysItr.next();
            JsonNode value = objectNode.get(key);
            Object resultValue = value.asText();
            if (value.isNull()) {
                resultValue = null;
            } else if (value.isObject()) {
                resultValue = toMap((ObjectNode) value);
            } else if (value.isArray()) {
                resultValue = toList((ArrayNode) value);
            }
            if (key.contains(SEGMENT_CHARACTER)) {
                String[] split = key.split(ESCAPED_SEGMENT_CHARACTER);
                resultValue = toMap(removeFirstSegmentOfPath(split), value);
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put(split[0], resultValue);
                map = deepMerge(map, resultMap);
            } else {
                map.put(key, resultValue);
            }
        }
        return map;
    }

    private static String removeFirstSegmentOfPath(String[] split) {
        if (split.length > 1) {
            return String.join(SEGMENT_CHARACTER, Arrays.asList(split).subList(1, split.length));
        } else {
            throw new IllegalArgumentException("Path only has one segment.");
        }
    }

    private static Map<String, Object> toMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        String[] split = key.split(ESCAPED_SEGMENT_CHARACTER);
        if (split.length == 1) {
            map.put(split[0], value);
        } else {
            Map<String, Object> stringObjectMap = toMap(removeFirstSegmentOfPath(split), value);
            if (map.keySet().contains(split[0]) && map.get(split[0]) instanceof Map) {
                Map<String, Object> mergedStringObjectMap = deepMerge((Map<String, Object>) map.get(split[0]), stringObjectMap);
                map.put(split[0], mergedStringObjectMap);
            } else {
                map.put(split[0], stringObjectMap);
            }
        }
        return map;
    }

    private static List<Object> toList(ArrayNode arrayNode) {
        List<Object> list = new ArrayList<>();

        for (JsonNode value : arrayNode) {
            Object resultValue = value.asText();
            if (value.isNull()) {
                resultValue = null;
            } else if (value.isObject()) {
                resultValue = toMap((ObjectNode) value);
            } else if (value.isArray()) {
                resultValue = toList((ArrayNode) value);
            }
            list.add(resultValue);
        }
        return list;
    }

    public static Map<String, Object> deepMerge(Map<String, Object> original, Map<String, Object> newMap) {
        for (String entry : newMap.keySet()) {
            String key = entry;
            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
                Map<String, Object> originalChild = (Map<String, Object>) original.get(entry);
                Map<String, Object> newChild = (Map<String, Object>) newMap.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }
}
