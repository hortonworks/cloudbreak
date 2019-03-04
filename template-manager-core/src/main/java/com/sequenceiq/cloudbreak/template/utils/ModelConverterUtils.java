package com.sequenceiq.cloudbreak.template.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

@Component
public class ModelConverterUtils {

    public static final String SEGMENT_CHARACTER = ".";

    public static final String ESCAPED_SEGMENT_CHARACTER = "\\.";

    private ModelConverterUtils() {
    }

    public static Map<String, Object> convert(Object object) {
        return jsonToMap(object);
    }

    private static Map<String, Object> jsonToMap(Object json) throws JSONException {
        Map<String, Object> retMap = new HashMap<>();
        if (json != null) {
            retMap = toMap(JSONObject.fromObject(json));
        }
        return retMap;
    }

    private static Map<String, Object> toMap(JSONObject object) throws JSONException {
        Map<String, Object> map = new HashMap<>();

        Iterator<String> keysItr = object.keys();
        while (keysItr.hasNext()) {
            Object key = keysItr.next();
            Object value = object.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            if (key.toString().contains(SEGMENT_CHARACTER)) {
                String[] split = key.toString().split(ESCAPED_SEGMENT_CHARACTER);
                value = toMap(replaceFirstSegmentOfKey(key.toString()), value);
                Map<String, Object> result = new HashMap<>();
                result.put(split[0], value);
                map = deepMerge(map, result);
            } else {
                map.put(key.toString(), value);
            }
        }
        return map;
    }

    private static String replaceFirstSegmentOfKey(String key) {
        String[] split = key.split(ESCAPED_SEGMENT_CHARACTER);
        return key.replace(split[0] + SEGMENT_CHARACTER, "");
    }

    private static Map<String, Object> toMap(String key, Object value) throws JSONException {
        Map<String, Object> map = new HashMap<>();
        String[] split = key.split(ESCAPED_SEGMENT_CHARACTER);
        if (split.length == 1) {
            map.put(split[0], value);
        } else {
            Map<String, Object> stringObjectMap = toMap(replaceFirstSegmentOfKey(key), value);
            if (map.keySet().contains(split[0]) && map.get(split[0]) instanceof Map) {
                Map<String, Object> stringObjectMap1 = deepMerge((Map<String, Object>) map.get(split[0]), stringObjectMap);
                map.put(split[0], stringObjectMap1);
            } else {
                map.put(split[0], stringObjectMap);
            }
        }
        return map;
    }

    private static List<Object> toList(JSONArray array) throws JSONException {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
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
