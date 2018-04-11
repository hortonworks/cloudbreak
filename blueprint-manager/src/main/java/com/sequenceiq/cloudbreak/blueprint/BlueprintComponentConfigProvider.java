package com.sequenceiq.cloudbreak.blueprint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.templateprocessor.HostgroupEntry;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateConfigurationEntry;
import com.sequenceiq.cloudbreak.templateprocessor.processor.PreparationObject;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateTextProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BlueprintComponentConfigProvider {

    default TemplateTextProcessor customTextManipulation(PreparationObject source, TemplateTextProcessor blueprintProcessor) {
        return blueprintProcessor;
    }

    default List<TemplateConfigurationEntry> getSettingsEntries(PreparationObject source, String blueprintProcessor) {
        return Lists.newArrayList();
    }

    default List<TemplateConfigurationEntry> getConfigurationEntries(PreparationObject source, String blueprintProcessor) throws IOException {
        return Lists.newArrayList();
    }

    default Map<HostgroupEntry, List<TemplateConfigurationEntry>> getHostgroupConfigurationEntries(PreparationObject source, String blueprintProcessor)
            throws IOException {
        return Maps.newHashMap();
    }

    default boolean additionalCriteria(PreparationObject source, String blueprintText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }
}
