package com.sequenceiq.it.cloudbreak.newway;

import java.util.HashMap;
import java.util.Map;

public class TestParameter {
    private static Map<String, String> parameters;

    private TestParameter() {
    }

    public static void init() {
        parameters = new HashMap<>();
    }

    public static String get(String key) {
        return parameters.get(key);
    }

    public static void put(String key, String value) {
        parameters.put(key, value);
    }

    public static void putAll(Map<String, String> all) {
        parameters.putAll(all);
    }
}
