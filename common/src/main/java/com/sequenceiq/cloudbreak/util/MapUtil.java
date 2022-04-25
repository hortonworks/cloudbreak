package com.sequenceiq.cloudbreak.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

public final class MapUtil {

    private MapUtil() {
    }

    public static Map<String, Object> cleanMap(Map<String, Object> input) {
        if (Objects.isNull(input)) {
            return new HashMap<>();
        } else {
            return input.entrySet().stream().filter(entry -> !Objects.isNull(entry.getValue())
                            && !"null".equals(entry.getValue())
                            && !Strings.isNullOrEmpty(entry.getValue().toString()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

}
