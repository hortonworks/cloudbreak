package com.sequenceiq.periscope.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CloneUtils {

    private CloneUtils() {
        throw new IllegalStateException();
    }

    public static Map<String, Map<String, String>> copy(Map<String, Map<String, String>> original) {
        Map<String, Map<String, String>> copy = new HashMap<>();
        if (original != null) {
            for (Entry<String, Map<String, String>> entry : original.entrySet()) {
                copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }
        return copy;
    }
}
