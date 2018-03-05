package com.sequenceiq.cloudbreak.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.sequenceiq.cloudbreak.blueprint.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;

public interface BlueprintProcessor {

    String addConfigEntries(String originalBlueprint, List<BlueprintConfigurationEntry> properties, boolean override);

    String addSettingsEntries(String originalBlueprint, List<BlueprintConfigurationEntry> properties, boolean override);

    Set<String> getComponentsInHostGroup(String blueprintText, String hostGroup);

    String extendBlueprintHostGroupConfiguration(String blueprintText, HostgroupConfigurations hostGroupConfig);

    String extendBlueprintHostGroupConfiguration(String blueprintText, HostgroupConfigurations hostGroupConfig, boolean forced);

    String extendBlueprintGlobalConfiguration(String blueprintText, SiteConfigurations globalConfig);

    String extendBlueprintGlobalConfiguration(String blueprintText, SiteConfigurations globalConfig, boolean forced);

    Set<String> getHostGroupsWithComponent(String blueprintText, String component);

    boolean componentExistsInBlueprint(String component, String blueprintText);

    boolean componentsExistsInBlueprint(Set<String> components, String blueprintText);

    String removeComponentFromBlueprint(String component, String blueprintText);

    String modifyHdpVersion(String originalBlueprint, String hdpVersion);

    String addComponentToHostgroups(String component, Collection<String> hostGroupNames, String blueprintText);

    Map<String, Set<String>> getComponentsByHostGroup(String blueprintText);

    String addConfigEntryStringToBlueprint(String blueprintText, String config, boolean forced);

    String addSettingsEntryStringToBlueprint(String blueprintText, String config, boolean forced);

    String addComponentToHostgroups(String blueprintText, String component, Predicate<String> addToHostgroup);

    boolean hivaDatabaseConfigurationExistsInBlueprint(String blueprintText);

    String setSecurityType(String blueprintText, String kerberos);
}
