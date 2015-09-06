package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

public interface BlueprintProcessor {

    String addConfigEntries(String originalBlueprint, List<BlueprintConfigurationEntry> properties);

    String addDefaultFs(String originalBlueprint, String defaultFs);

}
