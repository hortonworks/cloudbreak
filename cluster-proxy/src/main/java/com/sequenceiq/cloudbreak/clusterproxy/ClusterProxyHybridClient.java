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

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class ClusterProxyHybridClient {

    private static final String REMOTE_CLUSTER_LIST_ENVIRONMENT_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/listEnvironments";

    private static final String REMOTE_CLUSTER_GET_ENVIRONMENT_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/describeEnvironment";

    private static final String REMOTE_CLUSTER_GET_REMOTE_DATA_CONTEXT_CONFIG_PATH =
            "/proxy/%s/PvcControlPlane/api/v1/servicediscovery/describeDatalakeAsApiRemoteDataContext";

    private static final String REMOTE_CLUSTER_GET_DATALKE_SERVICES_CONFIG_PATH =
            "/proxy/%s/PvcControlPlane/api/v1/servicediscovery/describeDatalakeServices";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyHybridClient.class);

    private RestTemplate hybridRestTemplate;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Autowired
    ClusterProxyHybridClient(RestTemplate hybridRestTemplate) {
        this.hybridRestTemplate = hybridRestTemplate;
    }

    public ListEnvironmentsResponse listEnvironments(String clusterIdentifier, String userCrn) {
        String readConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_LIST_ENVIRONMENT_CONFIG_PATH, clusterIdentifier);
        try {
            LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
            Optional<ResponseEntity<ListEnvironmentsResponse>> response = measure(() ->
                            listEnvironmentsFromUrl(readConfigUrl, userCrn),
                    LOGGER,
                    "Query environments took {} ms from {}.", clusterIdentifier);
            LOGGER.info("Cluster proxy with remote cluster read configuration response: {}", response);
            return response.isEmpty() ? new ListEnvironmentsResponse() : response.get().getBody();
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

    public DescribeEnvironmentResponse getEnvironment(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_ENVIRONMENT_CONFIG_PATH,
                clusterIdentifier);
        try {
            LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
            Optional<ResponseEntity<DescribeEnvironmentResponse>> response = measure(() ->
                            getEnvironmentsFromUrl(getConfigUrl, userCrn, environmentCrn),
                    LOGGER,
                    "Query environment took {} ms from {} with crn {}.", clusterIdentifier, environmentCrn);
            LOGGER.info("Cluster proxy with remote cluster get environment configuration response: {}", response);
            return response.isEmpty() ? new DescribeEnvironmentResponse() : response.get().getBody();
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

    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_REMOTE_DATA_CONTEXT_CONFIG_PATH,
                clusterIdentifier);
        try {
            LOGGER.info("Reading Remote Data Context with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
            Optional<ResponseEntity<DescribeDatalakeAsApiRemoteDataContextResponse>> response = measure(() ->
                            getRemoteDataContextFromUrl(getConfigUrl, userCrn, environmentCrn),
                    LOGGER,
                    "Query Remote Data Context from {} with crn {} ms took {}.", clusterIdentifier, environmentCrn);
            LOGGER.info("Cluster proxy with remote cluster get Remote Data Context response: {}", response);
            return response.isEmpty() ? new DescribeDatalakeAsApiRemoteDataContextResponse() : response.get().getBody();
        } catch (RestClientResponseException e) {
            String message = String.format("Error getting Remote Data Context for environment '%s' with cluster proxy configuration for " +
                    "cluster identifier '%s', " + "Error Response Body '%s'", environmentCrn, clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error reading Remote Data Context for cluster identifier '%s' and " +
                    "environment crn '%s'", clusterIdentifier, environmentCrn);
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    public DescribeDatalakeServicesResponse getDatalakeServices(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_DATALKE_SERVICES_CONFIG_PATH,
                clusterIdentifier);
        try {
            LOGGER.info("Reading Datalake services with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
            Optional<ResponseEntity<DescribeDatalakeServicesResponse>> response = measure(() ->
                            getDatalakeServicesFromUrl(getConfigUrl, userCrn, environmentCrn),
                    LOGGER,
                    "Query Datalake from {} with crn {} ms took {}.", clusterIdentifier, environmentCrn);
            LOGGER.info("Cluster proxy with remote cluster get Datalake services response: {}", response);
            return response.isEmpty() ? new DescribeDatalakeServicesResponse() : response.get().getBody();
        } catch (RestClientResponseException e) {
            String message = String.format("Error getting Datalake services for environment '%s' with cluster proxy configuration for " +
                    "cluster identifier '%s', " + "Error Response Body '%s'", environmentCrn, clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error reading Datalake services for cluster identifier '%s' and " +
                    "environment crn '%s'", clusterIdentifier, environmentCrn);
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    private Optional<ResponseEntity<DescribeEnvironmentResponse>> getEnvironmentsFromUrl(String readConfigUrl, String userCrn, String environment) {
        try {
            DescribeEnvironmentRequest postRequest = new DescribeEnvironmentRequest();
            postRequest.setEnvironmentName(environment);
            postRequest.setOutputView(DescribeEnvironmentRequest.OutputViewEnum.FULL);

            return Optional.of(
                    hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(postRequest, userCrn), DescribeEnvironmentResponse.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return Optional.empty();
        }
    }

    private Optional<ResponseEntity<DescribeDatalakeAsApiRemoteDataContextResponse>> getRemoteDataContextFromUrl(String readConfigUrl, String userCrn,
            String environment) {
        try {
            DescribeDatalakeAsApiRemoteDataContextRequest postRequest = new DescribeDatalakeAsApiRemoteDataContextRequest();
            postRequest.setDatalake(environment);
            return Optional.of(
                    hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(postRequest, userCrn), DescribeDatalakeAsApiRemoteDataContextResponse.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return Optional.empty();
        }
    }

    private Optional<ResponseEntity<DescribeDatalakeServicesResponse>> getDatalakeServicesFromUrl(String readConfigUrl, String userCrn,
            String environment) {
        try {
            DescribeDatalakeServicesRequest postRequest = new DescribeDatalakeServicesRequest();
            postRequest.setClusterid(environment);
            return Optional.of(
                    hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(postRequest, userCrn), DescribeDatalakeServicesResponse.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return Optional.empty();
        }
    }

    private Optional<ResponseEntity<ListEnvironmentsResponse>> listEnvironmentsFromUrl(String readConfigUrl, String userCrn) {
        try {
            return Optional.of(
                    hybridRestTemplate.postForEntity(readConfigUrl,
                            requestEntity(new Object(), userCrn), ListEnvironmentsResponse.class));
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return Optional.empty();
        }
    }

    private HttpEntity<String> requestEntity(Object o, String userCrn) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-cdp-actor-crn", userCrn);
        return new HttpEntity<>(JsonUtil.writeValueAsString(o), headers);
    }

}
