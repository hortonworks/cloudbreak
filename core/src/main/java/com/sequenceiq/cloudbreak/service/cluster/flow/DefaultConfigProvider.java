package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;

public interface DefaultConfigProvider {

    List<BlueprintConfigurationEntry> getDefaultConfigs();
}
