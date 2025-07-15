package com.sequenceiq.cloudbreak.clusterproxy;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesRequest;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateRequest;
import com.cloudera.thunderhead.service.environments2api.model.GetRootCertificateResponse;
import com.cloudera.thunderhead.service.environments2api.model.ListEnvironmentsResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;

@Component
public class ClusterProxyHybridClient {

    private static final String REMOTE_CLUSTER_LIST_ENVIRONMENT_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/listEnvironments";

    private static final String REMOTE_CLUSTER_GET_ENVIRONMENT_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/describeEnvironment";

    private static final String REMOTE_CLUSTER_GET_REMOTE_DATA_CONTEXT_CONFIG_PATH =
            "/proxy/%s/PvcControlPlane/api/v1/servicediscovery/describeDatalakeAsApiRemoteDataContext";

    private static final String REMOTE_CLUSTER_GET_DATALKE_SERVICES_CONFIG_PATH = "/proxy/%s/PvcControlPlane/api/v1/servicediscovery/describeDatalakeServices";

    private static final String REMOTE_CLUSTER_GET_ROOT_CERTIFICATE_PATH = "/proxy/%s/PvcControlPlane/api/v1/environments2/getRootCertificate";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyHybridClient.class);

    @Inject
    @Qualifier("hybridRestTemplate")
    private RestTemplate hybridRestTemplate;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public ListEnvironmentsResponse listEnvironments(String clusterIdentifier, String userCrn) {
        String readConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_LIST_ENVIRONMENT_CONFIG_PATH, clusterIdentifier);
        LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
        return callClusterProxy(clusterIdentifier, readConfigUrl, userCrn, null, this::listEnvironmentsFromUrl);
    }

    public DescribeEnvironmentV2Response getEnvironment(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_ENVIRONMENT_CONFIG_PATH,
                clusterIdentifier);
        LOGGER.info("Reading remote cluster with cluster proxy configuration for cluster identifer: {}", clusterIdentifier);
        return callClusterProxy(clusterIdentifier, getConfigUrl, userCrn, environmentCrn, this::getEnvironmentsFromUrl);
    }

    public DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContext(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_REMOTE_DATA_CONTEXT_CONFIG_PATH,
                clusterIdentifier);
        LOGGER.info("Reading Remote Data Context with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
        return callClusterProxy(clusterIdentifier, getConfigUrl, userCrn, environmentCrn, this::getRemoteDataContextFromUrl);
    }

    public DescribeDatalakeServicesResponse getDatalakeServices(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_DATALKE_SERVICES_CONFIG_PATH,
                clusterIdentifier);
        LOGGER.info("Reading Datalake services with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
        return callClusterProxy(clusterIdentifier, getConfigUrl, userCrn, environmentCrn, this::getDatalakeServicesFromUrl);
    }

    public GetRootCertificateResponse getRootCertificate(String clusterIdentifier, String userCrn, String environmentCrn) {
        String getConfigUrl = String.format(clusterProxyConfiguration.getClusterProxyUrl() + REMOTE_CLUSTER_GET_ROOT_CERTIFICATE_PATH, clusterIdentifier);
        LOGGER.info("Reading root certificate with cluster proxy configuration for cluster Identifier: {}", clusterIdentifier);
        return callClusterProxy(clusterIdentifier, getConfigUrl, userCrn, environmentCrn, this::getRootCertificateFromUrl);
    }

    private <T> T callClusterProxy(String clusterIdentifier, String getConfigUrl, String userCrn, String environmentCrn,
            ClusterProxyHybridCaller<T> function) {
        try {
            T response = measure(() -> function.call(getConfigUrl, userCrn, environmentCrn),
                    LOGGER, "Query from {} with crn {} ms took {}.", clusterIdentifier, environmentCrn);
            LOGGER.info("Cluster proxy with remote cluster response: {}", response);
            return response;
        } catch (RestClientResponseException e) {
            String message = String.format("Error getting response for environment '%s' with cluster proxy configuration for " +
                    "cluster identifier '%s', " + "Error Response Body '%s'", environmentCrn, clusterIdentifier, e.getResponseBodyAsString());
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        } catch (Exception e) {
            String message = String.format("Error reading response for cluster identifier '%s' and " +
                    "environment crn '%s'", clusterIdentifier, environmentCrn);
            LOGGER.warn(message + " URL: " + getConfigUrl, e);
            throw new ClusterProxyException(message, e);
        }
    }

    private DescribeEnvironmentV2Response getEnvironmentsFromUrl(String readConfigUrl, String userCrn, String environment) {
        try {
            DescribeEnvironmentRequest postRequest = new DescribeEnvironmentRequest();
            postRequest.setEnvironmentName(environment);
            postRequest.setOutputView(DescribeEnvironmentRequest.OutputViewEnum.FULL);

            return hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(postRequest, userCrn), DescribeEnvironmentV2Response.class).getBody();
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return new DescribeEnvironmentV2Response();
        }
    }

    private DescribeDatalakeAsApiRemoteDataContextResponse getRemoteDataContextFromUrl(String readConfigUrl, String userCrn, String environment) {
        try {
            DescribeDatalakeAsApiRemoteDataContextRequest postRequest = new DescribeDatalakeAsApiRemoteDataContextRequest();
            postRequest.setDatalake(environment);
            return hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(postRequest, userCrn), DescribeDatalakeAsApiRemoteDataContextResponse.class)
                    .getBody();
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return new DescribeDatalakeAsApiRemoteDataContextResponse();
        }
    }

    private DescribeDatalakeServicesResponse getDatalakeServicesFromUrl(String readConfigUrl, String userCrn, String environment) {
        try {
            DescribeDatalakeServicesRequest postRequest = new DescribeDatalakeServicesRequest();
            postRequest.setClusterid(environment);
            return hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(postRequest, userCrn), DescribeDatalakeServicesResponse.class).getBody();
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return new  DescribeDatalakeServicesResponse();
        }
    }

    private ListEnvironmentsResponse listEnvironmentsFromUrl(String readConfigUrl, String userCrn, String environment) {
        try {
            return hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(new Object(), userCrn), ListEnvironmentsResponse.class).getBody();
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return new ListEnvironmentsResponse();
        }
    }

    private GetRootCertificateResponse getRootCertificateFromUrl(String readConfigUrl, String userCrn, String environment) {
        try {
            GetRootCertificateRequest request = new GetRootCertificateRequest().environmentName(environment);

            return hybridRestTemplate.postForEntity(readConfigUrl, requestEntity(request, userCrn), GetRootCertificateResponse.class).getBody();
        } catch (JsonProcessingException e) {
            LOGGER.warn("Error occurred when tried to parse the response json.", e);
            return new GetRootCertificateResponse();
        }
    }

    private HttpEntity<String> requestEntity(Object o, String userCrn) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-cdp-actor-crn", userCrn);
        return new HttpEntity<>(JsonUtil.writeValueAsString(o), headers);
    }

}
