package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
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
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.Tunnel;

@Component
public class ClusterProxyService {
    public static final String CB_INTERNAL = "cb-internal";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    public ConfigRegistrationResponse registerCluster(Stack stack) {
        ConfigRegistrationRequest proxyConfigRequest = createProxyConfigRequest(stack);
        ConfigRegistrationResponse configRegistrationResponse = clusterProxyRegistrationClient.registerConfig(proxyConfigRequest);
        stackUpdater.updateClusterProxyRegisteredFlag(stack, true);
        return configRegistrationResponse;
    }

    public ConfigRegistrationResponse reRegisterCluster(Stack stack) {
        ConfigRegistrationRequest proxyConfigRequest = createProxyConfigReRegisterRequest(stack);
        return clusterProxyRegistrationClient.registerConfig(proxyConfigRequest);
    }

    public void registerGatewayConfiguration(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (!stack.getCluster().hasGateway()) {
            LOGGER.warn("Cluster {} with crn {} in environment {} not configured with Gateway (Knox). Not updating Cluster Proxy with Gateway url.",
                    stack.getCluster().getName(), stack.getResourceCrn(), stack.getEnvironmentCrn());
            return;
        }
        registerGateway(stack);
    }

    public boolean useClusterProxyForCommunication(Tunnel tunnel) {
        return clusterProxyConfiguration.isClusterProxyIntegrationEnabled() && tunnel.useClusterProxy();
    }

    public boolean useClusterProxyForCommunication(Stack stack) {
        return useClusterProxyForCommunication(stack.getTunnel());
    }

    public boolean isCreateConfigForClusterProxy(Stack stack) {
        return useClusterProxyForCommunication(stack) && stack.isClusterProxyRegistered();
    }

    private void registerGateway(Stack stack) {
        ConfigUpdateRequest request = new ConfigUpdateRequest(stack.getResourceCrn(), knoxUrl(stack));
        clusterProxyRegistrationClient.updateConfig(request);
    }

    public void deregisterCluster(Stack stack) {
        stackUpdater.updateClusterProxyRegisteredFlag(stack, false);
        clusterProxyRegistrationClient.deregisterConfig(stack.getResourceCrn());
    }

    private ConfigRegistrationRequest createProxyConfigRequest(Stack stack) {
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .withAliases(singletonList(clusterId(stack.getCluster()))).withServices(serviceConfigs(stack));
        if (stack.getTunnel().useCcm()) {
            requestBuilder.withAccountId(getAccountId(stack)).withTunnelEntries(tunnelEntries(stack));
        }
        return requestBuilder.build();
    }

    private ConfigRegistrationRequest createProxyConfigReRegisterRequest(Stack stack) {
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .withAliases(singletonList(clusterId(stack.getCluster()))).withServices(serviceConfigs(stack)).withKnoxUrl(knoxUrl(stack));
        if (stack.getTunnel().useCcm()) {
            requestBuilder.withAccountId(getAccountId(stack)).withTunnelEntries(tunnelEntries(stack));
        }
        return requestBuilder.build();
    }

    private List<ClusterServiceConfig> serviceConfigs(Stack stack) {
        String internalAdminUrl = internalAdminUrl(stack, ServiceFamilies.GATEWAY.getDefaultPort());
        return asList(
                cmServiceConfig(stack, null, "cloudera-manager", clusterManagerUrl(stack)),
                cmServiceConfig(stack, clientCertificates(stack), CB_INTERNAL, internalAdminUrl));
    }

    private String getAccountId(Stack stack) {
        return Crn.safeFromString(stack.getResourceCrn()).getAccountId();
    }

    private List<TunnelEntry> tunnelEntries(Stack stack) {
        InstanceMetaData primaryGatewayInstance = stack.getPrimaryGatewayInstance();
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        TunnelEntry gatewayTunnel = new TunnelEntry(primaryGatewayInstance.getInstanceId(), KnownServiceIdentifier.GATEWAY.name(),
                gatewayIp, ServiceFamilies.GATEWAY.getDefaultPort(), stack.getMinaSshdServiceId());
        TunnelEntry knoxTunnel = new TunnelEntry(primaryGatewayInstance.getInstanceId(), KnownServiceIdentifier.KNOX.name(),
                gatewayIp, ServiceFamilies.KNOX.getDefaultPort(), stack.getMinaSshdServiceId());
        return asList(gatewayTunnel, knoxTunnel);
    }

    private ClusterServiceConfig cmServiceConfig(Stack stack, ClientCertificate clientCertificate, String serviceName, String clusterManagerUrl) {
        Cluster cluster = stack.getCluster();

        String cloudbreakUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakPasswordVaultPath = vaultPath(cluster.getCloudbreakAmbariPasswordSecret(), false);

        String dpUser = cluster.getDpAmbariUser();
        String dpPasswordVaultPath = vaultPath(cluster.getDpAmbariPasswordSecret(), false);

        List<ClusterServiceCredential> credentials = asList(new ClusterServiceCredential(cloudbreakUser, cloudbreakPasswordVaultPath),
                new ClusterServiceCredential(dpUser, dpPasswordVaultPath, true));
        return new ClusterServiceConfig(serviceName, singletonList(clusterManagerUrl), credentials, clientCertificate, null);
    }

    private ClientCertificate clientCertificates(Stack stack) {
        Optional<SecurityConfig> securityConfigOptional = securityConfigService.findOneByStackId(stack.getId());
        ClientCertificate clientCertificate = null;
        if (securityConfigOptional.isPresent()
                && StringUtils.isNoneBlank(securityConfigOptional.get().getClientCert(), securityConfigOptional.get().getClientCertSecret())) {
            SecurityConfig securityConfig = securityConfigOptional.get();
            String clientCertRef = vaultPath(securityConfig.getClientCertSecret(), true);
            String clientKeyRef = vaultPath(securityConfig.getClientKeySecret(), true);
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

    private String internalAdminUrl(Stack stack, int port) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPublicIpWrapper();
        return String.format("https://%s:%d", gatewayIp, port);
    }

    public String getProxyPath(String crn) {
        return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), crn, CB_INTERNAL);
    }

    private String vaultPath(String vaultSecretJsonString, boolean base64) {
        try {
            String path = JsonUtil.readValue(vaultSecretJsonString, VaultSecret.class).getPath() + ":secret";
            return base64 ? path + ":base64" : path;
        } catch (IOException e) {
            throw new VaultConfigException(String.format("Could not parse vault secret string '%s'", vaultSecretJsonString), e);
        }
    }
}
