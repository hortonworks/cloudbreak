package com.sequenceiq.cloudbreak.ambari;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public final class DataNodeUtils {

    private DataNodeUtils() {
        throw new IllegalStateException();
    }

    public static Map<String, Long> sortByUsedSpace(Map<String, Map<Long, Long>> dataNodes, boolean reverse) {
        Map<Long, List<String>> sorted = sort(dataNodes, reverse);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Entry<Long, List<String>> longListEntry : sorted.entrySet()) {
            List<String> hosts = longListEntry.getValue();
            for (String host : hosts) {
                result.put(host, longListEntry.getKey());
            }
        }
        return result;
    }

    private static Map<Long, List<String>> sort(Map<String, Map<Long, Long>> dataNodes, boolean reverse) {
        Map<Long, List<String>> result = getSortedMap(reverse);
        for (Entry<String, Map<Long, Long>> entry : dataNodes.entrySet()) {
            Map<Long, Long> usage = entry.getValue();
            long space = usage.values().iterator().next();
            List<String> hosts = result.get(space);
            if (hosts == null) {
                hosts = new ArrayList<>();
            }
            hosts.add(entry.getKey());
            result.put(space, hosts);
        }
        return result;
    }

    private static Map<Long, List<String>> getSortedMap(boolean reverse) {
        return reverse ? new TreeMap<>(reverseOrder()) : new TreeMap<>();
    }
}
