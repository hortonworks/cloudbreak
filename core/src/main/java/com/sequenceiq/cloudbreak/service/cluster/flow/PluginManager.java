package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.DockerContainer;

public interface PluginManager {

    void prepareKeyValues(Collection<InstanceMetaData> instanceMetaData, Map<String, String> keyValues);

    Map<String, Set<String>> installPlugins(Collection<InstanceMetaData> instanceMetaData, Map<String, PluginExecutionType> plugins, Set<String> hosts);

    void waitForEventFinish(Stack stack, Collection<InstanceMetaData> instanceMetaData, Map<String, Set<String>> eventIds, Integer timeout);

    void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event, Integer timeout, DockerContainer container);

    void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event, Integer timeout, DockerContainer container, List<String> payload, Set<String> hosts);
}
