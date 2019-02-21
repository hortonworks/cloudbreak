package com.sequenceiq.cloudbreak.template.processor;

import java.util.Map;
import java.util.Set;

public interface ClusterDefinitionTextProcessor {
    Map<String, Set<String>> getComponentsByHostGroup();

    ClusterManagerType getClusterManagerType();
}
