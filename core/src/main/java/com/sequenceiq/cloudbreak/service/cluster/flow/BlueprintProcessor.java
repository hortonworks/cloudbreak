package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;
import java.util.Set;

public interface BlueprintProcessor {

    String addConfigEntries(String originalBlueprint, List<BlueprintConfigurationEntry> properties, boolean override);

    Set<String> getComponentsInHostGroup(String blueprintText, String hostGroup);
}
