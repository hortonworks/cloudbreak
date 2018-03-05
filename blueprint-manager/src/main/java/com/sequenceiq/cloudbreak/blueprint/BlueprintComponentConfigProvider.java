package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public interface BlueprintComponentConfigProvider {

    default BlueprintTextProcessor customTextManipulation(BlueprintPreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        return blueprintProcessor;
    }

    default List<BlueprintConfigurationEntry> getSettingsEntries(BlueprintPreparationObject source, String blueprintProcessor) {
        return Lists.newArrayList();
    }

    default List<BlueprintConfigurationEntry> getConfigurationEntries(BlueprintPreparationObject source, String blueprintProcessor) throws IOException {
        return Lists.newArrayList();
    }

    default Map<HostgroupEntry, List<BlueprintConfigurationEntry>> getHostgroupConfigurationEntries(BlueprintPreparationObject source, String blueprintProcessor)
            throws IOException {
        return Maps.newHashMap();
    }

    default boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }
}
