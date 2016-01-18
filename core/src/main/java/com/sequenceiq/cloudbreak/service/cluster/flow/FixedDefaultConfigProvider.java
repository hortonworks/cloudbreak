package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FixedDefaultConfigProvider implements DefaultConfigProvider {

    @Override
    public List<BlueprintConfigurationEntry> getDefaultConfigs() {
        List<BlueprintConfigurationEntry> defaultConfigEntries = new ArrayList<>();
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.falcon.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.hbase.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.hcat.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.hive.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.oozie.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.root.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "proxyuser_group", "hadoop"));

        defaultConfigEntries.add(new BlueprintConfigurationEntry("hbase-site", "zookeeper.recovery.retry", "10"));

        return defaultConfigEntries;
    }
}
