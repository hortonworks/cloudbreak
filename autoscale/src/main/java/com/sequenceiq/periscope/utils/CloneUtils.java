package com.sequenceiq.periscope.utils;

import java.util.HashMap;
import java.util.Map;

public class CloneUtils {

    private CloneUtils() {
        throw new IllegalStateException();
    }

    public static Map<String, Map<String, String>> copy(Map<String, Map<String, String>> original) {
        Map<String, Map<String, String>> copy = new HashMap<>();
        if (original != null) {
            for (String key : original.keySet()) {
                copy.put(key, new HashMap<>(original.get(key)));
            }
        }
        return copy;
    }
}
