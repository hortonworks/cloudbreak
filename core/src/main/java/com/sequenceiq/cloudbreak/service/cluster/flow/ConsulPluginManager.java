package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.orchestrator.DockerContainer;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.cluster.PluginFailureException;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils;

@Component
public class ConsulPluginManager implements PluginManager {

    public static final String INSTALL_PLUGIN_EVENT = "install-plugin";
    public static final String FINISH_SIGNAL = "FINISHED";
    public static final String FAILED_SIGNAL = "FAILED";
    public static final int POLLING_INTERVAL = 5000;
    public static final int MAX_NODE_FILTER_LENGTH = 300;
    public static final int ONE_THOUSAND = 1000;
    public static final int SECONDS_IN_MINUTE = 60;

    @Inject
    private ConsulKVCheckerTask consulKVCheckerTask;

    @Inject
    private PollingService<ConsulKVCheckerContext> keyValuePollingService;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Override
    public void prepareKeyValues(TLSClientConfig clientConfig, Map<String, String> keyValues) {
        ConsulClient client = ConsulUtils.createClient(clientConfig);
        for (Map.Entry<String, String> kv : keyValues.entrySet()) {
            if (!ConsulUtils.putKVValue(Arrays.asList(client), kv.getKey(), kv.getValue(), null)) {
                throw new PluginFailureException("Failed to put values in Consul's key-value store.");
            }
        }
    }

    @Override
    public Map<String, Set<String>> installPlugins(TLSClientConfig clientConfig, Map<String, PluginExecutionType> plugins, Set<String> hosts) {
        Map<String, Set<String>> eventIdMap = new HashMap<>();
        ConsulClient client = ConsulUtils.createClient(clientConfig);
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
                String eventId = ConsulUtils.fireEvent(client, INSTALL_PLUGIN_EVENT, "TRIGGER_PLUGN " + plugin.getKey() + " " + getPluginName(plugin.getKey()),
                        eventParams, null);
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
            String shortHost = host.replace(ConsulUtils.CONSUL_DOMAIN, "");
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
    public void waitForEventFinish(Stack stack, Collection<InstanceMetaData> instanceMetaData, Map<String, Set<String>> eventIds, Integer timeout) {
        InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        TLSClientConfig clientConfig = new TLSClientConfig(gatewayInstance.getPublicIp(), stack.getCertDir());
        ConsulClient client = ConsulUtils.createClient(clientConfig);
        List<String> keys = generateKeys(eventIds);
        int calculatedMaxAttempt = (timeout * ONE_THOUSAND * SECONDS_IN_MINUTE) / POLLING_INTERVAL;
        keyValuePollingService.pollWithTimeout(
                consulKVCheckerTask,
                new ConsulKVCheckerContext(stack, client, keys, FINISH_SIGNAL, FAILED_SIGNAL),
                POLLING_INTERVAL, calculatedMaxAttempt
        );
    }

    @Override
    public void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event, Integer timeout, DockerContainer container) {
        triggerAndWaitForPlugins(stack, event, timeout, container, Collections.<String>emptyList(), null);
    }

    @Override
    public void triggerAndWaitForPlugins(Stack stack, ConsulPluginEvent event, Integer timeout, DockerContainer container,
            List<String> payload, Set<String> hosts) {
        Set<InstanceMetaData> instances = stack.getRunningInstanceMetaData();
        Set<String> targetHosts = hosts;
        if (hosts == null || hosts.isEmpty()) {
            targetHosts = getHostnames(hostMetadataRepository.findHostsInCluster(stack.getCluster().getId()));
        }
        InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
        TLSClientConfig clientConfig = new TLSClientConfig(gatewayInstance.getPublicIp(), stack.getCertDir());
        Map<String, Set<String>> triggerEventIds =
                triggerPlugins(clientConfig, event, container, payload, targetHosts);
        Map<String, Set<String>> eventIdMap = new HashMap<>();
        for (String eventId : triggerEventIds.keySet()) {
            Set<String> eventHosts = triggerEventIds.get(eventId);
            if (eventHosts.isEmpty()) {
                eventHosts = targetHosts;
            }
            eventIdMap.put(eventId, eventHosts);
        }
        waitForEventFinish(stack, instances, eventIdMap, timeout);
    }

    private Map<String, Set<String>> triggerPlugins(TLSClientConfig clientConfig, ConsulPluginEvent event,
            DockerContainer container, List<String> payload, Set<String> hosts) {
        ConsulClient client = ConsulUtils.createClient(clientConfig);
        if (hosts == null || hosts.isEmpty()) {
            return Collections.singletonMap(fireEvent(client, event, container, payload, null), Collections.<String>emptySet());
        }
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> nodeFilter : getNodeFilters(hosts).entrySet()) {
            EventParams eventParams = new EventParams();
            eventParams.setNode(nodeFilter.getKey());
            result.put(fireEvent(client, event, container, payload, eventParams), nodeFilter.getValue());
        }
        return result;
    }

    private String fireEvent(ConsulClient client, ConsulPluginEvent event, DockerContainer container, List<String> payload, EventParams eventParams) {
        String eventId = ConsulUtils.fireEvent(Arrays.asList(client), event.getName(),
                "TRIGGER_PLUGN_IN_CONTAINER " + container.getName() + " " + StringUtils.join(payload, " "), eventParams, null);
        if (eventId == null) {
            throw new PluginFailureException("Failed to trigger plugins, Consul client couldn't fire the "
                    + event.getName() + " event or failed to retrieve an event ID.");
        }
        return eventId;
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
