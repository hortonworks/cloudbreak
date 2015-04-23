package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.Collections.reverseOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DataNodeUtils {

    private DataNodeUtils() {
        throw new IllegalStateException();
    }

    public static Map<String, Long> sortByUsedSpace(Map<String, Map<Long, Long>> dataNodes, boolean reverse) {
        Map<Long, List<String>> sorted = sort(dataNodes, reverse);
        Map<String, Long> result = new LinkedHashMap<>();
        for (long space : sorted.keySet()) {
            List<String> hosts = sorted.get(space);
            for (String host : hosts) {
                result.put(host, space);
            }
        }
        return result;
    }

    private static Map<Long, List<String>> sort(Map<String, Map<Long, Long>> dataNodes, boolean reverse) {
        Map<Long, List<String>> result = getSortedMap(reverse);
        for (String hostName : dataNodes.keySet()) {
            Map<Long, Long> usage = dataNodes.get(hostName);
            long space = usage.values().iterator().next();
            List<String> hosts = result.get(space);
            if (hosts == null) {
                hosts = new ArrayList<>();
            }
            hosts.add(hostName);
            result.put(space, hosts);
        }
        return result;
    }

    private static Map<Long, List<String>> getSortedMap(boolean reverse) {
        if (reverse) {
            return new TreeMap<>(reverseOrder());
        } else {
            return new TreeMap<>();
        }
    }
}
