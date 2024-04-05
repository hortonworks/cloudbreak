package com.sequenceiq.cloudbreak.clusterproxy;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.RemoteEnvironmentResponses;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class ClusterProxyHybridClient {

    private static final String REMOTE_CLUSTER_READ_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/listEnvironments";

    private static final int CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_DELAY = 1000;

    private static final int CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_MULTIPLIER = 2;

    private static final int CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_MAX_DELAY = 10000;

    private static final int CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_MAX_ATTEMTPS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyHybridClient.class);

    private RestTemplate restTemplate;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Autowired
    ClusterProxyHybridClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(backoff = @Backoff(
            delay = CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_DELAY,
            multiplier = CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_MULTIPLIER,
            maxDelay = CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_MAX_DELAY),
            maxAttempts = CLUSTER_PROXY_REMOTE_CLUSTER_READ_RETRY_MAX_ATTEMTPS)
    public RemoteEnvironmentResponses readConfigWithRetry(String clusterIdentifier) {
        return readConfig(clusterIdentifier);
    }

    public RemoteEnvironmentResponses readConfig(String clusterIdentifier) {
        String readConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_READ_CONFIG_PATH, clusterIdentifier);
        try {
            LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
            Optional<ResponseEntity<RemoteEnvironmentResponses>> response = measure(() ->
                    readEnvironments(readConfigUrl),
                    LOGGER,
                    "Query environments from {} ms took {}.", clusterIdentifier);
            LOGGER.info("Cluster proxy with remote cluster read configuration response: {}", response);
            return response.isEmpty() ? new RemoteEnvironmentResponses() : response.get().getBody();
        } catch (RestClientResponseException e) {
            String message = String.format("Error reading cluster proxy configuration for cluster identifier '%s' from Remote Cluster, " +
                            "Error Response Body '%s'", clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.warn(message + " URL: " + readConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error reading cluster proxy configuration for cluster identifier '%s' from Remote Cluster.",
                    clusterIdentifier);
            LOGGER.warn(message + " URL: " + readConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    private Optional<ResponseEntity<RemoteEnvironmentResponses>> readEnvironments(String readConfigUrl) {
        try {
            return Optional.of(
                    restTemplate.postForEntity(readConfigUrl,
                            requestEntity(new Object()), RemoteEnvironmentResponses.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return Optional.empty();
        }
    }

    private HttpEntity<String> requestEntity(Object o) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(JsonUtil.writeValueAsString(o), headers);
    }
}
