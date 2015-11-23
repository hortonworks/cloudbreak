package com.sequenceiq.cloudbreak.converter.util;

import java.util.Collection;
import java.util.Map;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class PlatformConverterUtil {

    private PlatformConverterUtil() {
    }

    public static <T extends StringType> Map<String, String> convertDefaults(Map<Platform, T> vms) {
        Map<String, String> result = Maps.newHashMap();
        for (Map.Entry<Platform, T> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), entry.getValue().value());
        }
        return result;
    }

    public static <P extends StringType, T extends StringType, C extends Collection<T>> Map<String, Collection<String>> convertPlatformMap(Map<P, C> vms) {
        Map<String, Collection<String>> result = Maps.newHashMap();
        for (Map.Entry<P, C> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), convertList(entry.getValue()));
        }
        return result;
    }

    public static <T extends StringType> Collection<String> convertList(Collection<T> vmlist) {
        Collection<String> result = Lists.newArrayList();
        for (T item : vmlist) {
            result.add(item.value());
        }
        return result;
    }
}
