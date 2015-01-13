package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.event.model.Event;
import com.ecwid.consul.v1.event.model.EventParams;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

public final class ConsulUtils {

    private static final int CONSUL_CLIENTS = 3;

    private ConsulUtils() {
        throw new IllegalStateException();
    }

    public static List<CatalogService> getService(List<ConsulClient> clients, String serviceName) {
        for (ConsulClient consul : clients) {
            List<CatalogService> service = getService(consul, serviceName);
            if (!service.isEmpty()) {
                return service;
            }
        }
        return Collections.emptyList();
    }

    public static List<CatalogService> getService(ConsulClient client, String serviceName) {
        try {
            return client.getCatalogService(serviceName, QueryParams.DEFAULT).getValue();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static Map<String, String> getMembers(List<ConsulClient> clients) {
        for (ConsulClient client : clients) {
            Map<String, String> members = getMembers(client);
            if (!members.isEmpty()) {
                return members;
            }
        }
        return Collections.emptyMap();
    }

    public static Map<String, String> getMembers(ConsulClient client) {
        try {
            Map<String, String> result = new HashMap<>();
            List<Member> members = client.getAgentMembers().getValue();
            for (Member member : members) {
                result.put(member.getAddress(), member.getName());
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static String fireEvent(List<ConsulClient> clients, String event, String payload, EventParams eventParams, QueryParams queryParams) {
        for (ConsulClient client : clients) {
            String eventId = fireEvent(client, event, payload, eventParams, queryParams);
            if (eventId != null) {
                return eventId;
            }
        }
        return null;
    }

    public static String fireEvent(ConsulClient client, String event, String payload, EventParams eventParams, QueryParams queryParams) {
        try {
            Event response = client.eventFire(event, payload, eventParams, queryParams).getValue();
            return response.getId();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getKVValue(List<ConsulClient> clients, String key, QueryParams queryParams) {
        for (ConsulClient client : clients) {
            String value = getKVValue(client, key, queryParams);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String getKVValue(ConsulClient client, String key, QueryParams queryParams) {
        try {
            GetValue getValue = client.getKVValue(key, queryParams).getValue();
            return new String(Base64.decodeBase64(getValue.getValue()));
        } catch (Exception e) {
            return null;
        }
    }

    public static Boolean putKVValue(List<ConsulClient> clients, String key, String value, PutParams putParams) {
        for (ConsulClient client : clients) {
            Boolean result = putKVValue(client, key, value, putParams);
            if (result) {
                return result;
            }
        }
        return false;
    }

    public static Boolean putKVValue(ConsulClient client, String key, String value, PutParams putParams) {
        try {
            return client.setKVValue(key, value, putParams).getValue();
        } catch (Exception e) {
            return false;
        }
    }

    public static List<ConsulClient> createClients(Collection<InstanceMetaData> instancesMetaData) {
        return createClients(instancesMetaData, CONSUL_CLIENTS);
    }

    public static List<ConsulClient> createClients(Collection<InstanceMetaData> instancesMetaData, int numClients) {
        List<ConsulClient> clients = new ArrayList<>();
        List<InstanceMetaData> instanceList = new ArrayList<>(instancesMetaData);
        int size = instancesMetaData.size();
        List<InstanceMetaData> subList = size < numClients ? instanceList : instanceList.subList(0, numClients);
        for (InstanceMetaData metaData : subList) {
            clients.add(new ConsulClient(metaData.getPublicIp()));
        }
        return clients;
    }

}
