package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.catalog.CatalogClient;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.kv.KeyValueClient;
import com.ecwid.consul.v1.kv.model.GetValue;

public final class ConsulUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulUtils.class);

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private ConsulUtils() {
        throw new IllegalStateException("ConsulUtils not instanceable");
    }

    public static List<CatalogService> getService(Iterable<ConsulClient> clients, String serviceName) {
        for (ConsulClient consul : clients) {
            List<CatalogService> service = getService(consul, serviceName);
            if (!service.isEmpty()) {
                return service;
            }
        }
        return Collections.emptyList();
    }

    public static List<CatalogService> getService(CatalogClient client, String serviceName) {
        try {
            return client.getCatalogService(serviceName, QueryParams.DEFAULT).getValue();
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    public static String getKVValue(Iterable<ConsulClient> clients, String key, QueryParams queryParams) {
        for (ConsulClient client : clients) {
            String value = getKVValue(client, key, queryParams);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String getKVValue(KeyValueClient client, String key, QueryParams queryParams) {
        try {
            GetValue getValue = client.getKVValue(key, queryParams).getValue();
            return getValue == null ? null : new String(Base64.decodeBase64(getValue.getValue()));
        } catch (OperationException e) {
            LOGGER.info("Failed to get entry '{}' from Consul's key-value store. Status code: {}, Message: {}", key, e.getStatusCode(), e.getStatusMessage());
            return null;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to get entry '{}' from Consul's key-value store. Error message: {}", key, e.getMessage());
            return null;
        }
    }

    public enum ConsulServers {
        SINGLE_NODE_COUNT_LOW(1, 2, 1),
        NODE_COUNT_LOW(3, 1000, 3),
        NODE_COUNT_MEDIUM(1001, 5000, 5),
        NODE_COUNT_HIGH(5001, 100_000, 7);

        private final int min;
        private final int max;
        private final int consulServerCount;

        ConsulServers(int min, int max, int consulServerCount) {
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