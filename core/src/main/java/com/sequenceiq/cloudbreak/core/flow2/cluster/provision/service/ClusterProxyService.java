package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_BACKEND_ID_FORMAT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.CcmV2Config;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceCredential;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequestBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigUpdateRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
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

    private static final String CB_INTERNAL = "cb-internal";

    private static final String CLOUDERA_MANAGER_SERVICE_NAME = "cloudera-manager";

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
    private ClusterProxyEnablementService clusterProxyEnablementService;

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

    public boolean useClusterProxyForCommunication(Tunnel tunnel, String cloudPlatform) {
        return clusterProxyEnablementService.isClusterProxyApplicable(cloudPlatform) && tunnel.useClusterProxy();
    }

    public boolean useClusterProxyForCommunication(Stack stack) {
        return useClusterProxyForCommunication(stack.getTunnel(), stack.cloudPlatform());
    }

    public boolean isCreateConfigForClusterProxy(Stack stack) {
        return useClusterProxyForCommunication(stack) && stack.isClusterProxyRegistered();
    }

    private void registerGateway(Stack stack) {
        String knoxUrl = stack.getTunnel().useCcmV2OrJumpgate() ? knoxUrlForCcmV2(stack) : knoxUrlForNoCcmAndCcmV1(stack);
        ConfigUpdateRequest request = new ConfigUpdateRequest(stack.getResourceCrn(), knoxUrl);
        clusterProxyRegistrationClient.updateConfig(request);
    }

    public void deregisterCluster(Stack stack) {
        stackUpdater.updateClusterProxyRegisteredFlag(stack, false);
        clusterProxyRegistrationClient.deregisterConfig(stack.getResourceCrn());
    }

    private ConfigRegistrationRequest createProxyConfigRequest(Stack stack) {
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .withAliases(singletonList(clusterId(stack.getCluster())))
                .withServices(serviceConfigs(stack))
                .withAccountId(getAccountId(stack));
        if (stack.getTunnel().useCcmV1()) {
            requestBuilder.withTunnelEntries(tunnelEntries(stack));
        } else if (stack.getTunnel().useCcmV2()) {
            requestBuilder.withCcmV2Entries(ccmV2Configs(stack));
        } else if (stack.getTunnel().useCcmV2Jumpgate()) {
            requestBuilder.withEnvironmentCrn(stack.getEnvironmentCrn()).withUseCcmV2(true);
        }
        return requestBuilder.build();
    }

    private ConfigRegistrationRequest createProxyConfigReRegisterRequest(Stack stack) {
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .withAliases(singletonList(clusterId(stack.getCluster())))
                .withServices(serviceConfigs(stack))
                .withKnoxUrl(knoxUrlForNoCcmAndCcmV1(stack))
                .withAccountId(getAccountId(stack));
        if (stack.getTunnel().useCcmV1()) {
            requestBuilder.withTunnelEntries(tunnelEntries(stack));
        } else if (stack.getTunnel().useCcmV2()) {
            requestBuilder.withCcmV2Entries(ccmV2Configs(stack))
                    .withKnoxUrl(knoxUrlForCcmV2(stack));
        } else if (stack.getTunnel().useCcmV2Jumpgate()) {
            requestBuilder.withEnvironmentCrn(stack.getEnvironmentCrn())
                    .withUseCcmV2(true)
                    .withKnoxUrl(knoxUrlForCcmV2(stack));
        }
        return requestBuilder.build();
    }

    private List<ClusterServiceConfig> serviceConfigs(Stack stack) {
        boolean preferPrivateIp = privateIpShouldBePreferred(stack);
        String internalAdminUrl = internalAdminUrl(stack, ServiceFamilies.GATEWAY.getDefaultPort(), preferPrivateIp);
        LOGGER.info("Primary GW internal admin URL is: {}", internalAdminUrl);
        List<ClusterServiceConfig> clusterServiceConfigs = getClusterServiceConfigsForGWs(stack, preferPrivateIp);
        clusterServiceConfigs.add(cmServiceConfig(stack, clientCertificates(stack), CLOUDERA_MANAGER_SERVICE_NAME, internalAdminUrl));
        clusterServiceConfigs.add(cmServiceConfig(stack, clientCertificates(stack), CB_INTERNAL, internalAdminUrl));
        LOGGER.info("Service configs: {}", clusterServiceConfigs);
        return clusterServiceConfigs;
    }

    private List<ClusterServiceConfig> getClusterServiceConfigsForGWs(Stack stack, boolean preferPrivateIp) {
        List<InstanceMetaData> gatewayInstanceMetadatas = stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata();
        return gatewayInstanceMetadatas.stream()
                .map(gatewayInstanceMetadata -> createClusterServiceConfigFromGWMetadata(stack, gatewayInstanceMetadata, preferPrivateIp))
                .collect(Collectors.toList());
    }

    private ClusterServiceConfig createClusterServiceConfigFromGWMetadata(Stack stack, InstanceMetaData gatewayInstanceMetadata, boolean preferPrivateIp) {
        String gatewayIp = gatewayInstanceMetadata.getIpWrapper(preferPrivateIp);
        String internalAdminUrl = String.format("https://%s:%d", gatewayIp, ServiceFamilies.GATEWAY.getDefaultPort());
        return cmServiceConfig(stack, clientCertificates(stack),
                CB_INTERNAL + "-" + gatewayInstanceMetadata.getInstanceId(), internalAdminUrl);
    }

    private String getAccountId(Stack stack) {
        return Crn.safeFromString(stack.getResourceCrn()).getAccountId();
    }

    private List<TunnelEntry> tunnelEntries(Stack stack) {
        List<TunnelEntry> entries = new ArrayList<>();
        stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().forEach(md -> {
            String gatewayIp = md.getPrivateIp();
            TunnelEntry gatewayTunnel = new TunnelEntry(md.getInstanceId(), KnownServiceIdentifier.GATEWAY.name(),
                    gatewayIp, ServiceFamilies.GATEWAY.getDefaultPort(), stack.getMinaSshdServiceId());
            TunnelEntry knoxTunnel = new TunnelEntry(md.getInstanceId(), KnownServiceIdentifier.KNOX.name(),
                    gatewayIp, ServiceFamilies.KNOX.getDefaultPort(), stack.getMinaSshdServiceId());

            entries.add(gatewayTunnel);
            entries.add(knoxTunnel);
        });
        return entries;
    }

    private List<CcmV2Config> ccmV2Configs(Stack stack) {
        return stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .map(instanceMetaData -> new CcmV2Config(
                        stack.getCcmV2AgentCrn(),
                        instanceMetaData.getPrivateIp(),
                        ServiceFamilies.GATEWAY.getDefaultPort(),
                        String.format(CCMV2_BACKEND_ID_FORMAT, stack.getCcmV2AgentCrn(), instanceMetaData.getInstanceId()),
                        CLOUDERA_MANAGER_SERVICE_NAME))
                .collect(Collectors.toList());
    }

    private ClusterServiceConfig cmServiceConfig(Stack stack, ClientCertificate clientCertificate, String serviceName, String clusterManagerUrl) {
        Cluster cluster = stack.getCluster();

        String cloudbreakUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakPasswordVaultPath = vaultPath(cluster.getCloudbreakAmbariPasswordSecret(), false);

        String dpUser = cluster.getDpAmbariUser();
        String dpPasswordVaultPath = vaultPath(cluster.getDpAmbariPasswordSecret(), false);

        List<ClusterServiceCredential> credentials = asList(new ClusterServiceCredential(cloudbreakUser, cloudbreakPasswordVaultPath),
                new ClusterServiceCredential(dpUser, dpPasswordVaultPath, true));
        return new ClusterServiceConfig(serviceName, singletonList(clusterManagerUrl), credentials, clientCertificate);
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

    private String clusterId(Cluster cluster) {
        return cluster.getId().toString();
    }

    private String knoxUrlForNoCcmAndCcmV1(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getIpWrapper(stack.getTunnel().useCcmV1());
        Cluster cluster = stack.getCluster();
        String knoxUrl = String.format("https://%s/%s", gatewayIp, cluster.getGateway().getPath());
        LOGGER.info("The generated URL for Knox when the tunnel is direct ClusterProxy or CCMv1: '{}'", knoxUrl);
        return knoxUrl;
    }

    private String knoxUrlForCcmV2(Stack stack) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getPrivateIp();
        Cluster cluster = stack.getCluster();
        String knoxUrl = String.format("https://%s:%d/%s/%s", gatewayIp, ServiceFamilies.GATEWAY.getDefaultPort(),
                KnownServiceIdentifier.KNOX.toString().toLowerCase(),
                cluster.getGateway().getPath());
        LOGGER.info("The generated URL for Knox in case of CCMv2(.x): '{}'", knoxUrl);
        return knoxUrl;
    }

    private boolean privateIpShouldBePreferred(Stack stack) {
        return stack.getTunnel().useCcm();
    }

    private String internalAdminUrl(Stack stack, int port, boolean preferPrivateIp) {
        String gatewayIp = stack.getPrimaryGatewayInstance().getIpWrapper(preferPrivateIp);
        return String.format("https://%s:%d", gatewayIp, port);
    }

    public String getProxyPath(String crn, String internalIdentity) {
        String endpointWithIdentity = CB_INTERNAL + "-" + internalIdentity;
        LOGGER.info("Get proxy path for crn: {} and internal identity: {}", crn, internalIdentity);
        if (isServiceEndpointWithIdentityIsRegistered(crn, endpointWithIdentity)) {
            LOGGER.debug("Internal endpoint with identity is registered");
            return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), crn, endpointWithIdentity);
        } else {
            LOGGER.debug("Internal endpoint with identity is NOT registered");
            return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), crn, CB_INTERNAL);
        }
    }

    private boolean isServiceEndpointWithIdentityIsRegistered(String crn, String endpointWithIdentity) {
        ReadConfigResponse readConfigResponse = clusterProxyRegistrationClient.readConfig(crn);
        LOGGER.debug("Check if internal endpoint with identity is registered");
        return readConfigResponse.getServices().stream()
                .anyMatch(readConfigService -> endpointWithIdentity.equals(readConfigService.getName()));
    }

    private String vaultPath(String vaultSecretJsonString, boolean base64) {
        try {
            String path = JsonUtil.readValue(vaultSecretJsonString, VaultSecret.class).getPath() + ":secret";
            return base64 ? path + ":base64" : path;
        } catch (IOException e) {
            throw new VaultConfigException(String.format("Could not parse vault secret string '%s'", vaultSecretJsonString), e);
        }
    }

    public com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration getClusterProxyConfigurationForAutoscale() {
        return clusterProxyConfiguration.isClusterProxyIntegrationEnabled()
                ? com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration
                .enabled(clusterProxyConfiguration.getClusterProxyUrl())
                : com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.ClusterProxyConfiguration.disabled();
    }

    public void reRegisterCluster(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (clusterProxyEnablementService.isClusterProxyApplicable(stack.getCloudPlatform())) {
            LOGGER.info("Cluster Proxy integration is ENABLED, starting re-registering with Cluster Proxy service");
            reRegisterCluster(stack);
            LOGGER.info("Cluster has been re-registered with Cluster Proxy service successfully.");
        } else {
            LOGGER.debug("Cluster Proxy integration is DISABLED, skipping re-registering with Cluster Proxy service");
        }
    }
}
