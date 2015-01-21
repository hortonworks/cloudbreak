package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ecwid.consul.v1.ConsulClient;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.KeyValue;
import com.sequenceiq.cloudbreak.domain.Plugin;
import com.sequenceiq.cloudbreak.domain.Stack;
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

    @Autowired
    private PollingService<ConsulKVCheckerContext> keyValuePollingService;

    @Override
    public void prepareKeyValues(Collection<InstanceMetaData> instanceMetaData, Collection<KeyValue> keyValues) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        for (KeyValue kv : keyValues) {
            if (!ConsulUtils.putKVValue(clients, kv.getKey(), kv.getValue(), null)) {
                throw new PluginFailureException("Failed to put values in Consul's key-value store.");
            }
        }
    }

    @Override
    public Set<String> installPlugins(Collection<InstanceMetaData> instanceMetaData, Collection<Plugin> plugins) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        Set<String> eventIds = new HashSet<>();
        for (Plugin plugin : plugins) {
            String eventId = ConsulUtils.fireEvent(clients, INSTALL_PLUGIN_EVENT, plugin.getUrl() + " " + plugin.getName(), null, null);
            if (eventId != null) {
                eventIds.add(eventId);
            } else {
                throw new PluginFailureException("Failed to install plugins, Consul client couldn't fire the event or failed to retrieve an event ID."
                        + "Maybe the payload was too long (max. 512 bytes)?");
            }
        }
        return eventIds;
    }

    @Override
    public Set<String> triggerPlugins(Collection<InstanceMetaData> instanceMetaData, Collection<Plugin> plugins) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        Set<String> eventIds = new HashSet<>();
        for (Plugin plugin : plugins) {
            StringBuilder payload = new StringBuilder();
            for (String parameter : plugin.getParameters()) {
                payload.append(parameter).append(" ");
            }
            String eventId = ConsulUtils.fireEvent(clients, plugin.getName(), payload.toString().trim(), null, null);
            if (eventId != null) {
                eventIds.add(eventId);
            } else {
                throw new PluginFailureException("Failed to trigger plugins, Consul client couldn't fire the event or failed to retrieve an event ID.");
            }
        }
        return eventIds;
    }

    @Override
    public void waitForEventFinish(Stack stack, Collection<InstanceMetaData> instanceMetaData, Set<String> eventIds) {
        List<ConsulClient> clients = ConsulUtils.createClients(instanceMetaData);
        List<String> keys = generateKeys(instanceMetaData, eventIds);
        keyValuePollingService.pollWithTimeout(
                new ConsulKVCheckerTask(),
                new ConsulKVCheckerContext(stack, clients, keys, FINISH_SIGNAL, FAILED_SIGNAL),
                POLLING_INTERVAL, MAX_ATTEMPTS
        );
    }

    private List<String> generateKeys(Collection<InstanceMetaData> instanceMetaData, Set<String> eventIds) {
        List<String> keys = new ArrayList<>();
        for (String eventId : eventIds) {
            for (InstanceMetaData metaData : instanceMetaData) {
                keys.add(String.format("events/%s/%s", eventId, metaData.getLongName()));
            }
        }
        return keys;
    }
}
