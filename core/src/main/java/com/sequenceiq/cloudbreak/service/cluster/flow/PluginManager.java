package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.api.model.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;

public interface PluginManager {

    void prepareKeyValues(HttpClientConfig clientConfig, Map<String, String> keyValues);

    Map<String, Set<String>> installPlugins(HttpClientConfig clientConfig, Map<String, PluginExecutionType> plugins, Set<String> hosts,
            boolean existingHostGroup);

    Map<String, Set<String>> cleanupPlugins(HttpClientConfig clientConfig, Set<String> hosts);

    void waitForEventFinish(Stack stack, Collection<InstanceMetaData> instanceMetaData, Map<String, Set<String>> eventIds, Integer timeout)
            throws CloudbreakSecuritySetupException;

    void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event, Integer timeout, DockerContainer container) throws CloudbreakSecuritySetupException;

    void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event, Integer timeout, DockerContainer container, List<String> payload, Set<String> hosts)
            throws CloudbreakSecuritySetupException;
}
