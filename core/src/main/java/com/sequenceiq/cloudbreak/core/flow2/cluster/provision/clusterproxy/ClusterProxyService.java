package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterProxyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyService.class);

    private StackService stackService;

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
    ClusterProxyService(StackService stackService, RestTemplate restTemplate) {
        this.stackService = stackService;
        this.restTemplate = restTemplate;
    }

    public ConfigRegistrationResponse registerCluster(Stack stack) throws JsonProcessingException {
        try {
            ConfigRegistrationRequest proxyConfigRequest = createProxyConfigRequest(stack);
            LOGGER.debug("Cluster Proxy config request: {}", proxyConfigRequest);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(clusterProxyUrl + registerConfigPath,
                    requestEntity(proxyConfigRequest), ConfigRegistrationResponse.class);

            LOGGER.debug("Cluster Proxy config response: {}", response);
            return response.getBody();
        } catch (RestClientException e) {
            LOGGER.error("Error registering proxy configuration for cluster with stack crn {} and id {} with Cluster Proxy. URL: {}",
                    stack.getResourceCrn(), clusterId(stack.getCluster()), clusterProxyUrl + registerConfigPath, e);
            throw e;
        }
    }

    public ConfigRegistrationResponse reRegisterCluster(Stack stack) throws JsonProcessingException {
        try {
            ConfigRegistrationRequest proxyConfigRequest = createProxyConfigReRegisterRequest(stack);

            LOGGER.debug("Cluster Proxy config request: {}", proxyConfigRequest);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(clusterProxyUrl + registerConfigPath,
                    requestEntity(proxyConfigRequest), ConfigRegistrationResponse.class);

            LOGGER.debug("Cluster Proxy config response: {}", response);
            return response.getBody();
        } catch (RestClientException e) {
            LOGGER.error("Error re-registering proxy configuration for cluster with stack crn {} and id {} with Cluster Proxy. URL: {}",
                    stack.getResourceCrn(), clusterId(stack.getCluster()), clusterProxyUrl + registerConfigPath, e);
            throw e;
        }
    }

    public void registerGatewayConfiguration(Long stackId) throws JsonProcessingException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if  (!stack.getCluster().hasGateway()) {
            LOGGER.warn("Cluster {} with crn {} in environment {} not configured with Gateway (Knox). Not updating Cluster Proxy with Gateway url.",
                    stack.getCluster().getName(), stack.getResourceCrn(), stack.getEnvironmentCrn());
            return;
        }
        registerGateway(stack);
    }

    private void registerGateway(Stack stack) throws JsonProcessingException {
        try {
            ConfigUpdateRequest request = new ConfigUpdateRequest(stack.getResourceCrn(), knoxUrl(stack));
            LOGGER.debug("Cluster Proxy config update request: {}", request);
            ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(clusterProxyUrl + updateConfigPath,
                    requestEntity(request), ConfigRegistrationResponse.class);

            LOGGER.debug("Cluster Proxy config update response: {}", response);
        } catch (RestClientException e) {
            LOGGER.error("Error registering gateway configuration for cluster with stack crn {} and id {} with Cluster Proxy. URL: {}",
                    stack.getResourceCrn(), clusterId(stack.getCluster()), clusterProxyUrl + updateConfigPath, e);
            throw e;
        }
    }

    public void deregisterCluster(Stack stack) throws JsonProcessingException {
        String clusterId = clusterId(stack.getCluster());
        try {
            LOGGER.debug("Removing cluster proxy configuration for cluster with crn: {} and id: {}", stack.getResourceCrn(), clusterId);
            restTemplate.postForEntity(clusterProxyUrl + removeConfigPath,
                    requestEntity(new ConfigDeleteRequest(stack.getResourceCrn())), ConfigRegistrationResponse.class);
            LOGGER.debug("Removed cluster proxy configuration for cluster with crn: {} and id: {}", stack.getResourceCrn(), clusterId);
        } catch (RestClientException e) {
            LOGGER.error("Error de-registering proxy configuration for cluster with stack crn {} and id {} from Cluster Proxy. URL: {}",
                    stack.getResourceCrn(), clusterId, clusterProxyUrl + removeConfigPath, e);
            throw e;
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

    private ConfigRegistrationRequest createProxyConfigRequest(Stack stack) {
        return new ConfigRegistrationRequest(stack.getResourceCrn(), singletonList(clusterId(stack.getCluster())), singletonList(serviceConfig(stack)));
    }

    private ConfigRegistrationRequest createProxyConfigReRegisterRequest(Stack stack) {
        return new ConfigRegistrationRequest(stack.getResourceCrn(), knoxUrl(stack), singletonList(clusterId(stack.getCluster())),
                singletonList(serviceConfig(stack)));
    }

    private ClusterServiceConfig serviceConfig(Stack stack) {
        Cluster cluster = stack.getCluster();

        String cloudbreakUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakPasswordVaultPath = vaultPath(cluster.getCloudbreakAmbariPasswordSecret());

        String dpUser = cluster.getDpAmbariUser();
        String dpPasswordVaultPath = vaultPath(cluster.getDpAmbariPasswordSecret());

        List<ClusterServiceCredential> credentials = asList(new ClusterServiceCredential(cloudbreakUser, cloudbreakPasswordVaultPath),
                new ClusterServiceCredential(dpUser, dpPasswordVaultPath, true));
        return new ClusterServiceConfig("cloudera-manager", singletonList(clusterManagerUrl(stack)), credentials);
    }

    private String knoxUrl(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        Cluster cluster = stack.getCluster();
        return String.format("https://%s:8443/%s", gatewayIp, cluster.getGateway().getPath());
    }

    private String clusterId(Cluster cluster) {
        return cluster.getId().toString();
    }

    private String clusterManagerUrl(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        return String.format("https://%s/clouderamanager", gatewayIp);
    }

    private String vaultPath(String vaultSecretJsonString) {
        try {
            return JsonUtil.readValue(vaultSecretJsonString, VaultSecret.class).getPath() + ":secret";
        } catch (IOException e) {
            throw new VaultConfigException(String.format("Could not parse vault secret string '%s'", vaultSecretJsonString), e);
        }
    }
}
