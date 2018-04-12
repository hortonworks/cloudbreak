package com.sequenceiq.cloudbreak.blueprint;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.template.processor.HostgroupEntry;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateConfigurationEntry;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.processor.TemplateTextProcessor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface BlueprintComponentConfigProvider {

    default TemplateTextProcessor customTextManipulation(TemplatePreparationObject source, TemplateTextProcessor blueprintProcessor) {
        return blueprintProcessor;
    }

    default List<TemplateConfigurationEntry> getSettingsEntries(TemplatePreparationObject source, String blueprintProcessor) {
        return Lists.newArrayList();
    }

    default List<TemplateConfigurationEntry> getConfigurationEntries(TemplatePreparationObject source, String blueprintProcessor) throws IOException {
        return Lists.newArrayList();
    }

    default Map<HostgroupEntry, List<TemplateConfigurationEntry>> getHostgroupConfigurationEntries(TemplatePreparationObject source, String blueprintProcessor)
            throws IOException {
        return Maps.newHashMap();
    }

    default boolean additionalCriteria(TemplatePreparationObject source, String blueprintText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }
}
