package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceCredential;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequestBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigUpdateRequest;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterProxyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyService.class);

    private StackService stackService;

    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    private SecurityConfigService securityConfigService;

    @Autowired
    ClusterProxyService(StackService stackService, ClusterProxyRegistrationClient clusterProxyRegistrationClient, SecurityConfigService securityConfigService) {
        this.stackService = stackService;
        this.clusterProxyRegistrationClient = clusterProxyRegistrationClient;
        this.securityConfigService = securityConfigService;
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
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .with(singletonList(clusterId(stack.getCluster())), serviceConfigs(stack), null);
        if (Boolean.TRUE.equals(stack.getUseCcm())) {
            return requestBuilder.withAccountId(getAccountId(stack)).withTunnelEntries(tunnelEntries(stack)).build();
        }
        return requestBuilder.build();
    }

    private ConfigRegistrationRequest createProxyConfigReRegisterRequest(Stack stack) {
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .with(singletonList(clusterId(stack.getCluster())), serviceConfigs(stack), null)
                .withKnoxUrl(knoxUrl(stack));
        if (Boolean.TRUE.equals(stack.getUseCcm())) {
            return requestBuilder.withAccountId(getAccountId(stack)).withTunnelEntries(tunnelEntries(stack)).build();
        }
        return requestBuilder.build();
    }

    private List<ClusterServiceConfig> serviceConfigs(Stack stack) {
        String internalClusterManagerUrl = clusterManagerUrl(stack, ServiceFamilies.GATEWAY.getDefaultPort());
        return asList(
                cmServiceConfig(stack, null, "cloudera-manager", clusterManagerUrl(stack)),
                cmServiceConfig(stack, clientCertificates(stack), "cloudera-manager-internal", internalClusterManagerUrl));
    }

    private String getAccountId(Stack stack) {
        return Crn.safeFromString(stack.getResourceCrn()).getAccountId();
    }

    private List<TunnelEntry> tunnelEntries(Stack stack) {
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        int gatewayPort = ServiceFamilies.KNOX.getDefaultPort();
        return singletonList(new TunnelEntry(primaryGatewayInstance.getInstanceId(), KnownServiceIdentifier.GATEWAY.name(), gatewayIp, gatewayPort));
    }

    private ClusterServiceConfig cmServiceConfig(Stack stack, ClientCertificate clientCertificate, String serviceName, String clusterManagerUrl) {
        Cluster cluster = stack.getCluster();

        String cloudbreakUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakPasswordVaultPath = vaultPath(cluster.getCloudbreakAmbariPasswordSecret());

        String dpUser = cluster.getDpAmbariUser();
        String dpPasswordVaultPath = vaultPath(cluster.getDpAmbariPasswordSecret());

        List<ClusterServiceCredential> credentials = asList(new ClusterServiceCredential(cloudbreakUser, cloudbreakPasswordVaultPath),
                new ClusterServiceCredential(dpUser, dpPasswordVaultPath, true));
        return new ClusterServiceConfig(serviceName, singletonList(clusterManagerUrl), credentials, clientCertificate, null);
    }

    private ClientCertificate clientCertificates(Stack stack) {
        Optional<SecurityConfig> securityConfigOptional = securityConfigService.findOneByStackId(stack.getId());
        ClientCertificate clientCertificate = null;
        if (securityConfigOptional.isPresent()) {
            SecurityConfig securityConfig = securityConfigOptional.get();
            String clientCertRef = vaultPath(securityConfig.getClientCertSecret());
            String clientKeyRef =  vaultPath(securityConfig.getClientKeySecret());
            clientCertificate = new ClientCertificate(clientKeyRef, clientCertRef);
        }
        return clientCertificate;
    }

    private String knoxUrl(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        Cluster cluster = stack.getCluster();
        return String.format("https://%s/%s", gatewayIp, cluster.getGateway().getPath());
    }

    private String clusterId(Cluster cluster) {
        return cluster.getId().toString();
    }

    private String clusterManagerUrl(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        return String.format("https://%s/clouderamanager", gatewayIp);
    }

    private String clusterManagerUrl(Stack stack, int port) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        return String.format("https://%s:%d/clouderamanager", gatewayIp, port);
    }

    private String vaultPath(String vaultSecretJsonString) {
        try {
            return JsonUtil.readValue(vaultSecretJsonString, VaultSecret.class).getPath() + ":secret";
        } catch (IOException e) {
            throw new VaultConfigException(String.format("Could not parse vault secret string '%s'", vaultSecretJsonString), e);
        }
    }
}
