package com.sequenceiq.cloudbreak.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections4.MapUtils;

import com.google.common.base.Strings;

public final class MapUtil {

    private MapUtil() {
    }

    public static Map<String, Object> cleanMap(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        if (MapUtils.isNotEmpty(input)) {
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                if (!Objects.isNull(entry.getValue())
                        && !"null".equals(entry.getValue())
                        && !Strings.isNullOrEmpty(entry.getValue().toString())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

}
