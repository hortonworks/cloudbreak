package com.sequenceiq.cloudbreak.clusterproxy;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class ClusterProxyRegistrationClient {

    public static final int CLUSTER_PROXY_REGISTRATION_RETRY_DELAY = 1000;

    public static final int CLUSTER_PROXY_REGISTRATION_RETRY_MULTIPLIER = 2;

    public static final int CLUSTER_PROXY_REGISTRATION_RETRY_MAX_DELAY = 10000;

    public static final int CLUSTER_PROXY_REGISTRATION_RETRY_MAX_ATTEMTPS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRegistrationClient.class);

    @Qualifier("registrationRestTemplate")
    private RestTemplate restTemplate;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Autowired
    ClusterProxyRegistrationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(backoff = @Backoff(delay = CLUSTER_PROXY_REGISTRATION_RETRY_DELAY, multiplier = CLUSTER_PROXY_REGISTRATION_RETRY_MULTIPLIER,
            maxDelay = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_DELAY), maxAttempts = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_ATTEMTPS)
    public ConfigRegistrationResponse registerConfig(ConfigRegistrationRequest configRegistrationRequest) {
        String registerConfigUrl = clusterProxyConfiguration.getRegisterConfigUrl();
        try {
            LOGGER.info("Registering cluster proxy configuration: {}", configRegistrationRequest);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(registerConfigUrl,
                    requestEntity(configRegistrationRequest), ConfigRegistrationResponse.class);

            LOGGER.info("Cluster Proxy config registration response: {}", response);
            return response.getBody();
        } catch (RestClientResponseException e) {
            String message = String.format("Error registering proxy configuration for cluster '%s' with Cluster Proxy, " +
                            "Error Response Body '%s'", configRegistrationRequest.getClusterCrn(), e.getResponseBodyAsString());
            LOGGER.error(message + " URL: " + registerConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error registering proxy configuration for cluster '%s' with Cluster Proxy.",
                    configRegistrationRequest.getClusterCrn());
            LOGGER.error(message + " URL: " + registerConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    @Retryable(backoff = @Backoff(delay = CLUSTER_PROXY_REGISTRATION_RETRY_DELAY, multiplier = CLUSTER_PROXY_REGISTRATION_RETRY_MULTIPLIER,
            maxDelay = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_DELAY), maxAttempts = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_ATTEMTPS)
    public void updateConfig(ConfigUpdateRequest configUpdateRequest) {
        String updateConfigUrl = clusterProxyConfiguration.getUpdateConfigUrl();
        try {
            LOGGER.info("Updating cluster proxy configuration: {}", configUpdateRequest);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(updateConfigUrl,
                    requestEntity(configUpdateRequest), ConfigRegistrationResponse.class);
            LOGGER.info("Cluster Proxy config update response: {}", response);
        } catch (RestClientResponseException e) {
            String message = String.format("Error updating configuration for cluster '%s' with Cluster Proxy, " +
                            "Error Response Body '%s'", configUpdateRequest.getClusterCrn(), e.getResponseBodyAsString());
            LOGGER.error(message + " URL: " + updateConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error updating configuration for cluster '%s' with Cluster Proxy.",
                    configUpdateRequest.getClusterCrn());
            LOGGER.error(message + " URL: " + updateConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    @Retryable(backoff = @Backoff(delay = CLUSTER_PROXY_REGISTRATION_RETRY_DELAY, multiplier = CLUSTER_PROXY_REGISTRATION_RETRY_MULTIPLIER,
            maxDelay = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_DELAY), maxAttempts = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_ATTEMTPS)
    public void deregisterConfig(String clusterIdentifier) {
        String removeConfigUrl = clusterProxyConfiguration.getRemoveConfigUrl();
        try {
            LOGGER.info("Removing cluster proxy configuration for cluster identifier: {}", clusterIdentifier);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(removeConfigUrl,
                    requestEntity(new ConfigDeleteRequest(clusterIdentifier)), ConfigRegistrationResponse.class);
            LOGGER.info("Cluster proxy deregistration response: {}", response);
        } catch (RestClientResponseException e) {
            String message = String.format("Error de-registering proxy configuration for cluster identifier '%s' from Cluster Proxy, " +
                            "Error Response Body '%s'", clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.error(message + " URL: " + removeConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error de-registering proxy configuration for cluster identifier '%s' from Cluster Proxy.",
                    clusterIdentifier);
            LOGGER.error(message + " URL: " + removeConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    @Retryable(backoff = @Backoff(delay = CLUSTER_PROXY_REGISTRATION_RETRY_DELAY, multiplier = CLUSTER_PROXY_REGISTRATION_RETRY_MULTIPLIER,
            maxDelay = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_DELAY), maxAttempts = CLUSTER_PROXY_REGISTRATION_RETRY_MAX_ATTEMTPS)
    public ReadConfigResponse readConfig(String clusterIdentifier) {
        String readConfigUrl = clusterProxyConfiguration.getReadConfigUrl();
        try {
            LOGGER.info("Reading cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
            ResponseEntity<ReadConfigResponse> response = restTemplate.postForEntity(readConfigUrl,
                    requestEntity(new ReadConfigRequest(clusterIdentifier)), ReadConfigResponse.class);
            LOGGER.info("Cluster proxy read configuration response: {}", response);
            return response.getBody();
        } catch (RestClientResponseException e) {
            String message = String.format("Error reading cluster proxy configuration for cluster identifier '%s' from Cluster Proxy, " +
                            "Error Response Body '%s'", clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.error(message + " URL: " + readConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error reading cluster proxy configuration for cluster identifier '%s' from Cluster Proxy.",
                    clusterIdentifier);
            LOGGER.error(message + " URL: " + readConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    private HttpEntity<String> requestEntity(ConfigRegistrationRequest proxyConfigRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(JsonUtil.writeValueAsString(proxyConfigRequest), headers);
    }

    private HttpEntity<String> requestEntity(ConfigUpdateRequest proxyConfigRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(JsonUtil.writeValueAsString(proxyConfigRequest), headers);
    }

    private HttpEntity<String> requestEntity(ConfigDeleteRequest proxyConfigRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(JsonUtil.writeValueAsString(proxyConfigRequest), headers);
    }

    private HttpEntity<String> requestEntity(ReadConfigRequest proxyConfigRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(JsonUtil.writeValueAsString(proxyConfigRequest), headers);
    }
}
