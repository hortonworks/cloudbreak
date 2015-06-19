package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.agent.model.Member;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.event.model.Event;
import com.ecwid.consul.v1.event.model.EventParams;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.ecwid.consul.v1.kv.model.PutParams;

public final class ConsulUtils {

    public static final String CONSUL_DOMAIN = ".node.dc1.consul";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulUtils.class);

    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final int ALIVE_STATUS = 1;
    private static final int LEFT_STATUS = 3;
    private static final int GATEWAY_PORT = 443;

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

    public static Map<String, String> getAliveMembers(List<ConsulClient> clients) {
        return getMembers(clients, ALIVE_STATUS);
    }

    public static Map<String, String> getLeftMembers(List<ConsulClient> clients) {
        return getMembers(clients, LEFT_STATUS);
    }

    public static Map<String, String> getMembers(List<ConsulClient> clients, int status) {
        for (ConsulClient client : clients) {
            Map<String, String> members = getMembers(client, status);
            if (!members.isEmpty()) {
                return members;
            }
        }
        return Collections.emptyMap();
    }

    public static Map<String, String> getMembers(ConsulClient client, int status) {
        try {
            Map<String, String> result = new HashMap<>();
            List<Member> members = client.getAgentMembers().getValue();
            for (Member member : members) {
                if (member.getStatus() == status) {
                    result.put(member.getAddress(), member.getName());
                }
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
        } catch (OperationException e) {
            LOGGER.info("Failed to fire Consul event '{}'. Status code: {}, Message: {}", event, e.getStatusCode(), e.getStatusMessage());
            return null;
        } catch (Exception e) {
            LOGGER.info("Failed to fire Consul event '{}'. Message: {}", event, e.getMessage());
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
            return getValue == null ? null : new String(Base64.decodeBase64(getValue.getValue()));
        } catch (OperationException e) {
            LOGGER.info("Failed to get entry '{}' from Consul's key-value store. Status code: {}, Message: {}", key, e.getStatusCode(), e.getStatusMessage());
            return null;
        } catch (Exception e) {
            LOGGER.info("Failed to get entry '{}' from Consul's key-value store. Error message: {}", key, e.getMessage());
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
        } catch (OperationException e) {
            LOGGER.info("Failed to put entry '{}' in Consul's key-value store. Status code: {}, Message: {}", key, e.getStatusCode(), e.getStatusMessage());
            return false;
        } catch (Exception e) {
            LOGGER.info("Failed to put entry '{}' in Consul's key-value store. Error message: {}", key, e.getMessage());
            return false;
        }
    }

    public static ConsulClient createClient(TLSClientConfig tlsClientConfig) {
        return createClient(tlsClientConfig, DEFAULT_TIMEOUT_MS);
    }

    public static ConsulClient createClient(TLSClientConfig tlsClientConfig, int timeout) {
        return new ConsulClient("https://" + tlsClientConfig.getApiAddress(), GATEWAY_PORT,
                tlsClientConfig.getClientCert(),
                tlsClientConfig.getClientKey(),
                tlsClientConfig.getServerCert(),
                timeout);
    }

    public static void agentForceLeave(List<ConsulClient> clients, String nodeName) {
        for (ConsulClient client : clients) {
            try {
                client.agentForceLeave(nodeName);
            } catch (Exception e) {
                return;
            }
        }
    }

    public static int getConsulServerCount(int nodeCount) {
        if (nodeCount < ConsulServers.NODE_COUNT_LOW.getMax()) {
            return ConsulServers.NODE_COUNT_LOW.getConsulServerCount();
        } else if (nodeCount < ConsulServers.NODE_COUNT_MEDIUM.getMax()) {
            return ConsulServers.NODE_COUNT_MEDIUM.getConsulServerCount();
        } else {
            return ConsulServers.NODE_COUNT_HIGH.getConsulServerCount();
        }
    }

    public enum ConsulServers {
        NODE_COUNT_LOW(3, 1000, 3),
        NODE_COUNT_MEDIUM(1001, 5000, 5),
        NODE_COUNT_HIGH(5001, 100_000, 7);

        private final int min;
        private final int max;
        private final int consulServerCount;

        private ConsulServers(int min, int max, int consulServerCount) {
            this.min = min;
            this.max = max;
            this.consulServerCount = consulServerCount;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public int getConsulServerCount() {
            return consulServerCount;
        }
    }
}
