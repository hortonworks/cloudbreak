package com.sequenceiq.cloudbreak.clusterproxy;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class ClusterProxyProxyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyProxyClient.class);

    private final RestTemplate restTemplate;

    private final String clusterCrn;

    private final String serviceName;

    public ClusterProxyProxyClient(RestTemplate restTemplate, String clusterCrn, String serviceName) {
        this.restTemplate = restTemplate;
        this.clusterCrn = clusterCrn;
        this.serviceName = serviceName;
    }

    public <T> T get(String path, Class<T> responseType) {
        String fullPath = getFullPath(path);
        LOGGER.debug("Going through cluster proxy to GET {}", fullPath);
        return callClusterProxy(fullPath, () -> restTemplate.getForObject(fullPath, responseType));
    }

    public <T> T post(String path, Object request, Class<T> responseType) {
        String fullPath = getFullPath(path);
        LOGGER.debug("Going through cluster proxy to POST {}", fullPath);
        return callClusterProxy(fullPath, () -> restTemplate.postForObject(fullPath, JsonUtil.writeValueAsStringUnchecked(request), responseType));
    }

    private String getFullPath(String path) {
        return String.format("/%s/%s/%s", clusterCrn, serviceName, path);
    }

    public <T> T callClusterProxy(String fullPath, Supplier<T> callable) {
        try {
            return measure(callable, LOGGER, "Cluster proxied call took {} ms at {}", fullPath);
        } catch (RestClientResponseException e) {
            String message = String.format("Error getting response for cluster identifier '%s' with cluster proxy configuration for " +
                    "service '%s', Error Response Body '%s'", clusterCrn, serviceName, e.getResponseBodyAsString());
            LOGGER.warn("{} URL: {}", message, fullPath, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error getting response for cluster identifier '%s' with cluster proxy configuration for " +
                    "service '%s'", clusterCrn, serviceName);
            LOGGER.warn("{} URL: {}", message, fullPath, e);
            throw new ClusterProxyException(message, e);
        }
    }
}
