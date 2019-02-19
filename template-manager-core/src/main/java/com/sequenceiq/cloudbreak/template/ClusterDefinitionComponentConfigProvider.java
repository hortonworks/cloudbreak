package com.sequenceiq.cloudbreak.template;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.ClusterDefinitionConfigurationEntry;
import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupEntry;

public interface ClusterDefinitionComponentConfigProvider {

    default AmbariBlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, AmbariBlueprintTextProcessor blueprintProcessor) {
        return blueprintProcessor;
    }

    default List<ClusterDefinitionConfigurationEntry> getSettingsEntries(TemplatePreparationObject source, String clusterDefinitionProcessor) {
        return Lists.newArrayList();
    }

    default List<ClusterDefinitionConfigurationEntry> getConfigurationEntries(TemplatePreparationObject source, String clusterDefinitionProcessor) {
        return Lists.newArrayList();
    }

    default Map<HostgroupEntry, List<ClusterDefinitionConfigurationEntry>> getHostgroupConfigurationEntries(TemplatePreparationObject source,
            String clusterDefinitionProcessor) throws IOException {
        return Maps.newHashMap();
    }

    default boolean specialCondition(TemplatePreparationObject source, String clusterDefinitionText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }
}
