package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collections;
import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.catalog.model.CatalogService;

public final class ConsulUtils {

    private ConsulUtils() {
        throw new IllegalStateException();
    }

    public static List<CatalogService> getService(List<ConsulClient> client, String serviceName) {
        for (ConsulClient consul : client) {
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
}
