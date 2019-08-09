package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceCredential;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigUpdateRequest;
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

    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Autowired
    ClusterProxyService(StackService stackService, ClusterProxyRegistrationClient clusterProxyRegistrationClient) {
        this.stackService = stackService;
        this.clusterProxyRegistrationClient = clusterProxyRegistrationClient;
    }

    public ConfigRegistrationResponse registerCluster(Stack stack) {
            ConfigRegistrationRequest proxyConfigRequest = createProxyConfigRequest(stack);
            return clusterProxyRegistrationClient.registerConfig(proxyConfigRequest);
    }

    public ConfigRegistrationResponse reRegisterCluster(Stack stack) {
            ConfigRegistrationRequest proxyConfigRequest = createProxyConfigReRegisterRequest(stack);
            return clusterProxyRegistrationClient.registerConfig(proxyConfigRequest);
    }

    public void registerGatewayConfiguration(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if  (!stack.getCluster().hasGateway()) {
            LOGGER.warn("Cluster {} with crn {} in environment {} not configured with Gateway (Knox). Not updating Cluster Proxy with Gateway url.",
                    stack.getCluster().getName(), stack.getResourceCrn(), stack.getEnvironmentCrn());
            return;
        }
        registerGateway(stack);
    }

    private void registerGateway(Stack stack) {
            ConfigUpdateRequest request = new ConfigUpdateRequest(stack.getResourceCrn(), knoxUrl(stack));
            clusterProxyRegistrationClient.updateConfig(request);
    }

    public void deregisterCluster(Stack stack) {
        clusterProxyRegistrationClient.deregisterConfig(stack.getResourceCrn());
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
