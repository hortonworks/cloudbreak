package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;

public interface DatalakeConfigApi {

    String YARN = "YARN";

    Map<String, String> getConfigValuesByConfigIds(List<String> configIds);

    List<String> getHostNamesByComponent(String component);

    Map<String, Map<String, String>> getServiceComponentsMap();
}
