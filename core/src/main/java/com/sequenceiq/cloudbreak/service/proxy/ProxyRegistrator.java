package com.sequenceiq.cloudbreak.service.proxy;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;

@Service
public class ProxyRegistrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyRegistrator.class);

    private static final String BACKEND_URL = "traefik/backends/%s/servers/gw/url";

    private static final String FRONTED_BASE = "traefik/frontends/%s/";

    private static final String FRONTED_WIRE_TO_BACKEND = FRONTED_BASE + "backend";

    private static final String FRONTED_PASS_HOST_HEADER = FRONTED_BASE + "passHostHeader";

    private static final String FRONTED_RULE = FRONTED_BASE + "routes/gw/rule";

    @Inject
    private ConsulClient consulClient;

    @Retryable(value = OperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 2000))
    public void register(String clusterName, String contextPath, String gatewayHost) {
        registerKeys(clusterName, String.format("https://%s:8443", gatewayHost), String.format("/%s/", contextPath));
    }

    @Retryable(value = OperationException.class, maxAttempts = 10, backoff = @Backoff(delay = 2000))
    public void remove(String clusterName) {
        removeKeys(clusterName);
    }

    private void registerKeys(String clusterName, String baseUrl, String context) {
        LOGGER.info("Register cluster to Consul: clusterName: {}, baseUrl: {}, context: {}",
                clusterName, baseUrl, context);
        consulClient.setKVValue(String.format(BACKEND_URL, clusterName), baseUrl + context);
        consulClient.setKVValue(String.format(FRONTED_WIRE_TO_BACKEND, clusterName), clusterName);
        consulClient.setKVValue(String.format(FRONTED_PASS_HOST_HEADER, clusterName), "true");
        consulClient.setKVValue(String.format(FRONTED_RULE, clusterName), String.format("PathPrefix:%s", context));
    }

    private void removeKeys(String clusterName) {
        LOGGER.info("Remove cluster from Consul: clusterName: {}", clusterName);
        consulClient.deleteKVValues(String.format("traefik/backends/%s", clusterName));
        consulClient.deleteKVValues(String.format("traefik/frontends/%s", clusterName));
    }

    private void removeAll() {
        consulClient.deleteKVValues("traefik/backends");
        consulClient.deleteKVValues("traefik/frontends");
    }

}
