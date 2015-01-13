package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Collection;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.KeyValue;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface PluginManager {

    void prepareKeyValues(Collection<InstanceMetaData> instanceMetaData, Collection<KeyValue> keyValues);

    Set<String> installPlugins(Collection<InstanceMetaData> instanceMetaData, Collection<Plugin> plugins);

    Set<String> triggerPlugins(Collection<InstanceMetaData> instanceMetaData, Collection<Plugin> plugins);

    void waitForEventFinish(Stack stack, Collection<InstanceMetaData> instanceMetaData, Set<String> eventIds);
}
