package com.sequenceiq.cloudbreak.template.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;

public interface ClusterDefinitionTextProcessor {
    Map<String, Set<String>> getComponentsByHostGroup();

    ClusterManagerType getClusterManagerType();

    ClusterDefinitionTextProcessor replaceConfiguration(String s, String descriptor);

    ClusterDefinitionTextProcessor addComponentToHostgroups(String component, Predicate<String> addToHostgroup);

    ClusterDefinitionTextProcessor addComponentToHostgroups(String component, Collection<String> addToHostGroups);

    ClusterDefinitionTextProcessor setSecurityType(String kerberos);

    ClusterDefinitionTextProcessor extendBlueprintGlobalConfiguration(SiteConfigurations configs, boolean forced);

    ClusterDefinitionTextProcessor extendBlueprintHostGroupConfiguration(HostgroupConfigurations hostgroupConfigurations, boolean b);

    Set<String> getHostGroupsWithComponent(String component);

    String asText();

    Set<String> getComponentsInHostGroup(String name);
}
