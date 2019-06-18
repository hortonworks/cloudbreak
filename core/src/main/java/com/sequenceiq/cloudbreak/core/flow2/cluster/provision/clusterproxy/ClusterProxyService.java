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

    @Autowired
    ClusterProxyService(StackService stackService, RestTemplate restTemplate) {
        this.stackService = stackService;
        this.restTemplate = restTemplate;
    }

    public ConfigRegistrationResponse registerProxyConfiguration(Long stackId) throws JsonProcessingException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        ConfigRegistrationRequest proxyConfigRequest = createProxyConfigRequest(stack);
        LOGGER.debug("Cluster Proxy config request: {}", proxyConfigRequest);
        ResponseEntity<ConfigRegistrationResponse> response = restTemplate.postForEntity(clusterProxyUrl + registerConfigPath,
                requestEntity(proxyConfigRequest), ConfigRegistrationResponse.class);

        LOGGER.debug("Cluster Proxy config response: {}", response);
        return response.getBody();
    }

    private HttpEntity<String> requestEntity(ConfigRegistrationRequest proxyConfigRequest) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(JsonUtil.writeValueAsString(proxyConfigRequest), headers);
    }

    private ConfigRegistrationRequest createProxyConfigRequest(Stack stack) {
        Cluster cluster = stack.getCluster();

        String cloudbreakUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakPasswordVaultPath = vaultPath(cluster.getCloudbreakAmbariPasswordSecret());

        String dpUser = cluster.getDpAmbariUser();
        String dpPasswordVaultPath = vaultPath(cluster.getDpAmbariPasswordSecret());

        List<ClusterServiceCredential> credentials = asList(new ClusterServiceCredential(cloudbreakUser, cloudbreakPasswordVaultPath),
                new ClusterServiceCredential(dpUser, dpPasswordVaultPath));
        ClusterServiceConfig serviceConfig = new ClusterServiceConfig("cloudera-manager", singletonList(clusterManagerUrl(stack)), credentials);
        return new ConfigRegistrationRequest(stack.getCluster().getId().toString(), singletonList(serviceConfig));
    }

    private String clusterManagerUrl(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        Integer gatewayPort = stack.getGatewayPort();
        return String.format("https://%s:%d", gatewayIp, gatewayPort);
    }

    private String vaultPath(String vaultSecretJsonString) {
        try {
            return JsonUtil.readValue(vaultSecretJsonString, VaultSecret.class).getPath();
        } catch (IOException e) {
            throw new VaultConfigException(String.format("Could not parse vault secret string '%s'", vaultSecretJsonString), e);
        }
    }
}
