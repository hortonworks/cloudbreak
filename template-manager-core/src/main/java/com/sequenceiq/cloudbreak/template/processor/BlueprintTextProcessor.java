package com.sequenceiq.cloudbreak.template.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.sequenceiq.cloudbreak.template.processor.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;

public interface BlueprintTextProcessor {
    Map<String, Set<String>> getComponentsByHostGroup();

    ClusterManagerType getClusterManagerType();

    BlueprintTextProcessor replaceConfiguration(String s, String descriptor);

    BlueprintTextProcessor addComponentToHostgroups(String component, Predicate<String> addToHostgroup);

    BlueprintTextProcessor addComponentToHostgroups(String component, Collection<String> addToHostGroups);

    boolean isComponentExistsInHostGroup(String component, String hostGroup);

    BlueprintTextProcessor setSecurityType(String kerberos);

    BlueprintTextProcessor extendBlueprintGlobalConfiguration(SiteConfigurations configs, boolean forced);

    BlueprintTextProcessor extendBlueprintHostGroupConfiguration(HostgroupConfigurations hostgroupConfigurations, boolean b);

    Set<String> getHostGroupsWithComponent(String component);

    String asText();

    Set<String> getComponentsInHostGroup(String name);
}
