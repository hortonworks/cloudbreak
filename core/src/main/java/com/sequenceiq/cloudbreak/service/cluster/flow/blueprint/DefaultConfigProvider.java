package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.List;

public interface DefaultConfigProvider {

    List<BlueprintConfigurationEntry> getDefaultConfigs();
}
