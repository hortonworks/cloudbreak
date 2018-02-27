package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public interface BlueprintComponentConfigProvider {

    default String customTextManipulation(BlueprintPreparationObject source, String blueprintText) {
        return blueprintText;
    }

    default List<BlueprintConfigurationEntry> getSettingsEntries(BlueprintPreparationObject source, String blueprintText) {
        return Lists.newArrayList();
    }

    default List<BlueprintConfigurationEntry> getConfigurationEntries(BlueprintPreparationObject source, String blueprintText) throws IOException {
        return Lists.newArrayList();
    }

    default Map<HostgroupEntry, List<BlueprintConfigurationEntry>> getHostgroupConfigurationEntries(BlueprintPreparationObject source, String blueprintText)
            throws IOException {
        return Maps.newHashMap();
    }

    default boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }

    default boolean ldapConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        return false;
    }

    default List<BlueprintConfigurationEntry> ldapConfigs(BlueprintPreparationObject source, String blueprintText) {
        return Lists.newArrayList();
    }

    default boolean rdsConfigShouldApply(BlueprintPreparationObject source, String blueprintText) {
        return false;
    }

    default List<BlueprintConfigurationEntry> rdsConfigs(BlueprintPreparationObject source, String blueprintText) {
        return Lists.newArrayList();
    }
}
