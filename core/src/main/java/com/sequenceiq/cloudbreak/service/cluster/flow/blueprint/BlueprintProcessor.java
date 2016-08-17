package com.sequenceiq.cloudbreak.service.cluster.flow.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface BlueprintProcessor {

    String addConfigEntries(String originalBlueprint, List<BlueprintConfigurationEntry> properties, boolean override);

    String addSettingsEntries(String originalBlueprint, List<BlueprintConfigurationEntry> properties, boolean override);

    Set<String> getComponentsInHostGroup(String blueprintText, String hostGroup);

    boolean componentExistsInBlueprint(String component, String blueprintText);

    String removeComponentFromBlueprint(String component, String blueprintText);

    String modifyHdpVersion(String originalBlueprint, String hdpVersion);

    String addComponentToHostgroups(String component, Collection<String> hostGroupNames, String blueprintText);
}
