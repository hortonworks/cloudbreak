package com.sequenceiq.cloudbreak.template;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupEntry;

public interface BlueprintComponentConfigProvider {

    default BlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        return blueprintProcessor;
    }

    default List<BlueprintConfigurationEntry> getSettingsEntries(TemplatePreparationObject source, String blueprintProcessor) {
        return Lists.newArrayList();
    }

    default List<BlueprintConfigurationEntry> getConfigurationEntries(TemplatePreparationObject source, String blueprintProcessor) {
        return Lists.newArrayList();
    }

    default Map<HostgroupEntry, List<BlueprintConfigurationEntry>> getHostgroupConfigurationEntries(TemplatePreparationObject source,
            String blueprintProcessor) {
        return Maps.newHashMap();
    }

    default boolean specialCondition(TemplatePreparationObject source, String blueprintText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }
}
