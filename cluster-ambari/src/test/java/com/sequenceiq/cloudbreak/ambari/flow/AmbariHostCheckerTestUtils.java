package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class AmbariHostCheckerTestUtils {

    private AmbariHostCheckerTestUtils() {
    }

    public static Map<String, Map<String, String>> getHostComponentStates(List<Map<String, String>> componentStates) {
        Map<String, Map<String, String>> hostComponentsStates = new HashMap<>();
        int index = 1;
        for (Map<String, String> componentState : componentStates) {
            hostComponentsStates.put("host" + index++, componentState);
        }
        return hostComponentsStates;
    }

    public static Map<String, String> getComponentStates(String... states) {
        return getMapWithStates("component", states);
    }

    public static Map<String, String> getHostStatuses(String... states) {
        return getMapWithStates("host", states);
    }

    private static Map<String, String> getMapWithStates(String prefix, String... states) {
        Map<String, String> compStates = new HashMap<>();
        int index = 1;
        for (String state : states) {
            compStates.put(prefix + index++, state);
        }
        return compStates;
    }

    public static Set<InstanceMetaData> getMetadataSet(int count) {
        Set<InstanceMetaData> result = new HashSet<>();
        IntStream.range(1, count + 1).forEach(value -> {
            InstanceMetaData metadata = new InstanceMetaData();
            metadata.setDiscoveryFQDN("host" + value);
            result.add(metadata);
        });
        return result;
    }

}
