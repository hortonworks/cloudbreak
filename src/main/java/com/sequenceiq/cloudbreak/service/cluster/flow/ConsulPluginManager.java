package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.event.model.EventParams;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.PluginExecutionType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.PluginFailureException;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Component
public class ConsulPluginManager implements PluginManager {

    public static final String INSTALL_PLUGIN_EVENT = "install-plugin";
    public static final String FINISH_SIGNAL = "FINISHED";
    public static final String FAILED_SIGNAL = "FAILED";
    public static final int POLLING_INTERVAL = 5000;
    public static final int MAX_ATTEMPTS = 30;
    private static final int MAX_NODE_FILTER_LENGTH = 500;

    @Autowired
    private ConsulKVCheckerTask consulKVCheckerTask;

    @Autowired
    private PollingService<ConsulKVCheckerContext> keyValuePollingService;

    @Autowired
    private HostMetadataRepository hostMetadataRepository;

    @Override
    public void prepareKeyValues(Collection<InstanceMetaData> instanceMetaData, Map<String, String> keyValues) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        for (Map.Entry<String, String> kv : keyValues.entrySet()) {
            if (!ConsulUtils.putKVValue(clients, kv.getKey(), kv.getValue(), null)) {
                throw new PluginFailureException("Failed to put values in Consul's key-value store.");
            }
        }
    }

    @Override
    public Map<String, Set<String>> installPlugins(Collection<InstanceMetaData> instanceMetaData, Map<String, PluginExecutionType> plugins, Set<String> hosts) {
        Map<String, Set<String>> eventIdMap = new HashMap<>();
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        for (Map.Entry<String, PluginExecutionType> plugin : plugins.entrySet()) {
            Set<String> installedHosts = new HashSet<>();
            if (PluginExecutionType.ONE_NODE.equals(plugin.getValue())) {
                String installedHost = FluentIterable.from(hosts).first().get();
                installedHosts.add(installedHost);
            } else {
                installedHosts.addAll(hosts);
            }
            for (Map.Entry<String, Set<String>> nodeFilter : getNodeFilters(installedHosts).entrySet()) {
                EventParams eventParams = new EventParams();
                eventParams.setNode(nodeFilter.getKey());
                String eventId = ConsulUtils.fireEvent(clients, INSTALL_PLUGIN_EVENT, plugin.getKey() + " " + getPluginName(plugin.getKey()), eventParams, null);
                if (eventId != null) {
                    eventIdMap.put(eventId, nodeFilter.getValue());
                } else {
                    throw new PluginFailureException("Failed to install plugins, Consul client couldn't fire the event or failed to retrieve an event ID."
                            + "Maybe the payload was too long (max. 512 bytes)?");
                }
            }
        }
        return eventIdMap;
    }

    private Map<String, Set<String>> getNodeFilters(Set<String> hosts) {
        Map<String, Set<String>> nodeFilters = new HashMap<>();
        StringBuilder nodeFilter = new StringBuilder("");
        Set<String> hostsForNodeFilter = new HashSet<>();
        for (String host : hosts) {
            String shortHost = host.replace(".node.consul", "");
            if (nodeFilter.length() >= MAX_NODE_FILTER_LENGTH - shortHost.length()) {
                nodeFilters.put(nodeFilter.deleteCharAt(nodeFilter.length() - 1).toString(), Sets.newHashSet(hostsForNodeFilter));
                nodeFilter.setLength(0);
                hostsForNodeFilter.clear();
            }
            hostsForNodeFilter.add(host);
            nodeFilter.append(shortHost).append("|");
        }
        nodeFilters.put(nodeFilter.deleteCharAt(nodeFilter.length() - 1).toString(), Sets.newHashSet(hostsForNodeFilter));
        return nodeFilters;
    }

    @Override
    public Set<String> triggerPlugins(Collection<InstanceMetaData> instanceMetaData, ConsulPluginEvent event) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        String eventId = ConsulUtils.fireEvent(clients, event.getName(), "", null, null);
        if (eventId != null) {
            return Sets.newHashSet(eventId);
        } else {
            throw new PluginFailureException("Failed to trigger plugins, Consul client couldn't fire the "
                    + event.getName() + " event or failed to retrieve an event ID.");
        }
    }

    @Override
    public void waitForEventFinish(Stack stack, Collection<InstanceMetaData> instanceMetaData, Map<String, Set<String>> eventIds) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        List<String> keys = generateKeys(eventIds);
        keyValuePollingService.pollWithTimeout(
                consulKVCheckerTask,
                new ConsulKVCheckerContext(stack, clients, keys, FINISH_SIGNAL, FAILED_SIGNAL),
                POLLING_INTERVAL, MAX_ATTEMPTS
        );
    }

    @Override
    public void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event) {
        Set<InstanceMetaData> instances = stack.getRunningInstanceMetaData();
        Set hosts = getHostnames(hostMetadataRepository.findHostsInCluster(stack.getCluster().getId()));
        Set<String> triggerEventIds = triggerPlugins(instances, event);
        Map<String, Set<String>> eventIdMap = new HashMap<>();
        for (String eventId : triggerEventIds) {
            eventIdMap.put(eventId, hosts);
        }
        waitForEventFinish(stack, instances, eventIdMap);
    }

    private List<String> generateKeys(Map<String, Set<String>> eventIds) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Set<String>> event : eventIds.entrySet()) {
            for (String host : event.getValue()) {
                keys.add(String.format("events/%s/%s", event.getKey(), host));
            }
        }
        return keys;
    }

    private String getPluginName(String url) {
        String[] splits = url.split("/");
        return splits[splits.length - 1].replace("consul-plugins-", "").replace(".git", "");
    }

    private Set<String> getHostnames(Set<HostMetadata> hostMetadata) {
        return FluentIterable.from(hostMetadata).transform(new Function<HostMetadata, String>() {
            @Nullable
            @Override
            public String apply(@Nullable HostMetadata metadata) {
                return metadata.getHostName();
            }
        }).toSet();
    }
}
