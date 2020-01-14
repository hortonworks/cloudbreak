package com.sequenceiq.cloudbreak.common.cost;

import java.util.Map;

@FunctionalInterface
public interface CostTagging {

    Map<String, String> prepareDefaultTags(String cbUser, Map<String, String> sourceMap, String platform, String environmentName);

}
