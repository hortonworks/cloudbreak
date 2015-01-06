package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
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
