package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FixedDefaultConfigProvider implements DefaultConfigProvider {

    @Override
    public List<BlueprintConfigurationEntry> getDefaultConfigs() {
        List<BlueprintConfigurationEntry> defaultConfigEntries = new ArrayList<>();
        defaultConfigEntries.add(new BlueprintConfigurationEntry("mapred-site", "mapreduce.map.memory.mb", "1536"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("mapred-site", "mapreduce.reduce.memory.mb", "3072"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("mapred-site", "mapreduce.map.java.opts", "-Xmx1228m"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("mapred-site", "mapreduce.reduce.java.opts", "-Xmx2457m"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("mapred-site", "mapreduce.task.io.sort.mb", "614"));

        defaultConfigEntries.add(new BlueprintConfigurationEntry("yarn-site", "yarn.scheduler.minimum-allocation-mb", "1536"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("yarn-site", "yarn.scheduler.maximum-allocation-mb", "6144"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("yarn-site", "yarn.nodemanager.resource.memory-mb", "6144"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("mapred-site", "yarn.app.mapreduce.am.resource.mb", "3072"));
        defaultConfigEntries
                .add(new BlueprintConfigurationEntry("mapred-site", "yarn.app.mapreduce.am.command-opts", "-Xmx2457m -Dhdp.version=${hdp.version}"));

        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.falcon.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.hbase.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.hcat.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.hive.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.oozie.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "hadoop.proxyuser.root.groups", "*"));
        defaultConfigEntries.add(new BlueprintConfigurationEntry("core-site", "proxyuser_group", "hadoop"));

        return defaultConfigEntries;
    }
}
