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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.DescribeRemoteEnvironmentRequest;
import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.OutputView;
import com.sequenceiq.cloudbreak.clusterproxy.remoteenvironment.RemoteEnvironmentResponses;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class ClusterProxyHybridClient {

    private static final String REMOTE_CLUSTER_LIST_ENVIRONMENT_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/listEnvironments";

    private static final String REMOTE_CLUSTER_GET_ENVIRONMENT_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/describeEnvironment";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyHybridClient.class);

    private RestTemplate restTemplate;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Autowired
    ClusterProxyHybridClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RemoteEnvironmentResponses listEnvironments(String clusterIdentifier) {
        String readConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_LIST_ENVIRONMENT_CONFIG_PATH, clusterIdentifier);
        try {
            LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
            Optional<ResponseEntity<RemoteEnvironmentResponses>> response = measure(() ->
                    listEnvironmentsFromUrl(readConfigUrl),
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

    public Object getEnvironment(String clusterIdentifier, String pvcAccountId, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_ENVIRONMENT_CONFIG_PATH,
                clusterIdentifier);
        try {
            LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
            Optional<ResponseEntity<Object>> response = measure(() ->
                            getEnvironmentsFromUrl(getConfigUrl, pvcAccountId, environmentCrn),
                    LOGGER,
                    "Query environment from {} with crn {} ms took {}.", clusterIdentifier, environmentCrn);
            LOGGER.info("Cluster proxy with remote cluster get environment configuration response: {}", response);
            return response.isEmpty() ? new RemoteEnvironmentResponses() : response.get().getBody();
        } catch (RestClientResponseException e) {
            String message = String.format("Error getting environment '%s' cluster proxy configuration for cluster identifier '%s' from Remote Cluster, " +
                    "Error Response Body '%s'", environmentCrn, clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error reading cluster proxy configuration for cluster identifier '%s' and " +
                            "environment crn '%s' from Remote Cluster.", clusterIdentifier, environmentCrn);
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    private Optional<ResponseEntity<Object>> getEnvironmentsFromUrl(String readConfigUrl, String accountId, String environment) {
        try {
            DescribeRemoteEnvironmentRequest postRequest = new DescribeRemoteEnvironmentRequest();
            postRequest.setEnvironment(environment);
            postRequest.setAccountId(accountId);
            postRequest.setOutputView(OutputView.FULL);

            return Optional.of(
                    restTemplate.postForEntity(readConfigUrl,
                            requestEntity(postRequest), Object.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return Optional.empty();
        }
    }

    private Optional<ResponseEntity<RemoteEnvironmentResponses>> listEnvironmentsFromUrl(String readConfigUrl) {
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
