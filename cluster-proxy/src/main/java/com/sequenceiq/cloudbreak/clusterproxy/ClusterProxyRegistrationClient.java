package com.sequenceiq.cloudbreak.clusterproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class ClusterProxyRegistrationClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRegistrationClient.class);

    private RestTemplate restTemplate;

    @Value("${clusterProxy.url:}")
    private String clusterProxyUrl;

    @Value("${clusterProxy.registerConfigPath:}")
    private String registerConfigPath;

    @Value("${clusterProxy.updateConfigPath:}")
    private String updateConfigPath;

    @Value("${clusterProxy.removeConfigPath:}")
    private String removeConfigPath;

    @Autowired
    ClusterProxyRegistrationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ConfigRegistrationResponse registerConfig(ConfigRegistrationRequest configRegistrationRequest) {
        try {
            LOGGER.debug("Registering cluster proxy configuration: {}", configRegistrationRequest);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(clusterProxyUrl + registerConfigPath,
                    requestEntity(configRegistrationRequest), ConfigRegistrationResponse.class);

            LOGGER.debug("Cluster Proxy config registration response: {}", response);
            return response.getBody();
        } catch (Exception e) {
            String message = String.format("Error registering proxy configuration for cluster '%s' with Cluster Proxy. URL: '%s'",
                    configRegistrationRequest.getClusterCrn(), clusterProxyUrl + registerConfigPath);
            LOGGER.error(message, e);
            throw new ClusterProxyException(message, e);

        }
    }

    public void updateConfig(ConfigUpdateRequest configUpdateRequest) {
        try {
            LOGGER.debug("Updating cluster proxy configuration: {}", configUpdateRequest);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(clusterProxyUrl + updateConfigPath,
                    requestEntity(configUpdateRequest), ConfigRegistrationResponse.class);
            LOGGER.debug("Cluster Proxy config update response: {}", response);
        } catch (Exception e) {
            String message = String.format("Error updating configuration for cluster '%s' with Cluster Proxy. URL: '%s'",
                    configUpdateRequest.getClusterCrn(), clusterProxyUrl + updateConfigPath);
            LOGGER.error(message, e);
            throw new ClusterProxyException(message, e);
        }
    }

    public void deregisterConfig(String clusterIdentifier) {
        try {
            LOGGER.debug("Removing cluster proxy configuration for cluster identifier: {}", clusterIdentifier);
            restTemplate.postForEntity(clusterProxyUrl + removeConfigPath,
                    requestEntity(new ConfigDeleteRequest(clusterIdentifier)), ConfigRegistrationResponse.class);
            LOGGER.debug("Removed cluster proxy configuration for cluster identifier: {}", clusterIdentifier);
        } catch (Exception e) {
            String message = String.format("Error de-registering proxy configuration for cluster identifier '%s' from Cluster Proxy. URL: '%s'",
                    clusterIdentifier, clusterProxyUrl + removeConfigPath);
            LOGGER.error(message, e);
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
}
