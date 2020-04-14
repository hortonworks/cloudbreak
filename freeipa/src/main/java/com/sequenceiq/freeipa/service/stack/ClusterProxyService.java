package com.sequenceiq.freeipa.service.stack;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequestBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.util.ClusterProxyServiceAvailabilityChecker;
import com.sequenceiq.freeipa.vault.FreeIpaCertVaultComponent;

@Service
public class ClusterProxyService {

    private static final String FREEIPA_SERVICE_NAME = "freeipa";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyService.class);

    private static final String VAULT_KEY_SUFFIX = ":secret";

    private static final Boolean NO_TLS_STRICT_CHECK = false;

    private static final Boolean USE_TUNNEL = true;

    private static final String GATEWAY_SERVICE_TYPE = "GATEWAY";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Inject
    private FreeIpaCertVaultComponent freeIpaCertVaultComponent;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private FreeIpaService freeIpaService;

    public Optional<ConfigRegistrationResponse> registerFreeIpa(String accountId, String environmentCrn) {
        return registerFreeIpa(stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId), false);
    }

    public Optional<ConfigRegistrationResponse> updateFreeIpaRegistration(Long stackId) {
        return registerFreeIpa(stackService.getStackById(stackId), false);
    }

    public Optional<ConfigRegistrationResponse> registerBootstrapFreeIpa(Long stackId) {
        return registerFreeIpa(stackService.getStackById(stackId), true);
    }

    private Optional<ConfigRegistrationResponse> registerFreeIpa(Stack stack, boolean bootstrap) {

        if (!clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
            LOGGER.debug("Cluster Proxy integration disabled. Skipping registering FreeIpa [{}]", stack);
            return Optional.empty();
        }

        LOGGER.debug("Registering freeipa with cluster-proxy: Environment CRN = [{}], Stack CRN = [{}]", stack.getEnvironmentCrn(), stack.getResourceCrn());

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotTerminatedGatewayConfigs(stack);
        ClientCertificate clientCertificate = clientCertificates(stack);

        boolean usePrivateIpToTls = stack.getSecurityConfig().isUsePrivateIpToTls();
        List<GatewayConfig> tunnelGatewayConfigs;
        List<ClusterServiceConfig> serviceConfigs = new LinkedList<>();
        serviceConfigs.add(createServiceConfig(stack, FREEIPA_SERVICE_NAME, primaryGatewayConfig, clientCertificate, usePrivateIpToTls));

        if (bootstrap) {
            tunnelGatewayConfigs = List.of(primaryGatewayConfig);
        } else if (ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack)) {
            serviceConfigs.addAll(createDnsMappedServiceConfigs(stack, gatewayConfigs, clientCertificate, usePrivateIpToTls));
            tunnelGatewayConfigs = gatewayConfigs;
        } else {
            tunnelGatewayConfigs = List.of();
        }

        LOGGER.debug("Registering service configs [{}]", serviceConfigs);
        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .withServices(serviceConfigs)
                .withTunnelEntries(createTunnelEntries(stack, tunnelGatewayConfigs))
                .withAccountId(stack.getAccountId());
        ConfigRegistrationResponse response = clusterProxyRegistrationClient.registerConfig(requestBuilder.build());

        stackUpdater.updateClusterProxyRegisteredFlag(stack, true);

        return Optional.of(response);
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

    public void deregisterFreeIpa(String accountId, String environmentCrn) {
        deregisterFreeIpa(stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId));
    }

    public void deregisterFreeIpa(Long stackId) {
        deregisterFreeIpa(stackService.getStackById(stackId));
    }

    private void deregisterFreeIpa(Stack stack) {
        if (!clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
            LOGGER.debug("Cluster Proxy integration disabled. Skipping deregistering FreeIpa [{}]", stack);
            return;
        }
        LOGGER.debug("Deregistering freeipa with cluster-proxy: Environment CRN = [{}], Stack CRN = [{}]", stack.getEnvironmentCrn(), stack.getResourceCrn());
        stackUpdater.updateClusterProxyRegisteredFlag(stack, false);
        clusterProxyRegistrationClient.deregisterConfig(stack.getResourceCrn());
        LOGGER.debug("Cleaning up vault secrets for cluster-proxy");
        freeIpaCertVaultComponent.cleanupSecrets(stack);
    }

    private ClusterServiceConfig createServiceConfig(Stack stack, String serviceName, GatewayConfig gatewayConfig, ClientCertificate clientCertificate,
            boolean usePrivateIpToTls) {
        return new ClusterServiceConfig(serviceName,
                List.of(getEndpointForRegistration(stack, gatewayConfig, usePrivateIpToTls)),
                List.of(),
                clientCertificate,
                NO_TLS_STRICT_CHECK
        );
    }

    private List<ClusterServiceConfig> createDnsMappedServiceConfigs(Stack stack, List<GatewayConfig> gatewayConfigs, ClientCertificate clientCertificate,
            boolean usePrivateIpToTls) {
        return gatewayConfigs.stream()
                .map(gatewayConfig -> createServiceConfig(stack, gatewayConfig.getHostname(), gatewayConfig, clientCertificate, usePrivateIpToTls))
                .collect(Collectors.toList());
    }

    private List<TunnelEntry> createTunnelEntries(Stack stack, List<GatewayConfig> gatewayConfigs) {
        if (stack.getTunnel().useCcm()) {
            return gatewayConfigs.stream()
                    .map(gatewayConfig -> new TunnelEntry(
                            gatewayConfig.getInstanceId(),
                            GATEWAY_SERVICE_TYPE,
                            gatewayConfig.getPrivateAddress(),
                            gatewayConfig.getGatewayPort(),
                            stack.getMinaSshdServiceId()))
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public String getProxyPath(String crn) {
        return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), crn, FREEIPA_SERVICE_NAME);
    }

    public String getProxyPath(Stack stack, String serviceName) {
        if (ClusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack)) {
            return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), stack.getResourceCrn(), serviceName);
        } else {
            return getProxyPath(stack.getResourceCrn());
        }
    }

    private String vaultPath(String vaultSecretJsonString) {
        try {
            return JsonUtil.readValue(vaultSecretJsonString, VaultSecret.class).getPath() + ":secret:base64";
        } catch (IOException e) {
            throw new VaultConfigException(String.format("Could not parse vault secret string '%s'", vaultSecretJsonString), e);
        }
    }

    private ClientCertificate clientCertificates(Stack stack) {
        SecurityConfig securityConfig = securityConfigService.findOneByStack(stack);
        ClientCertificate clientCertificate = null;
        if (securityConfig != null
                && StringUtils.isNoneBlank(securityConfig.getClientCertVaultSecret(), securityConfig.getClientKeyVaultSecret())) {
            String clientCertRef = vaultPath(securityConfig.getClientCertVaultSecret());
            String clientKeyRef =  vaultPath(securityConfig.getClientKeyVaultSecret());
            clientCertificate = new ClientCertificate(clientKeyRef, clientCertRef);
        }
        return clientCertificate;
    }

    private String getEndpointForRegistration(Stack stack, GatewayConfig gatewayConfig, boolean usePrivateIpToTls) {
        String ipAddresss = gatewayConfig.getPublicAddress();
        if (usePrivateIpToTls) {
            ipAddresss = gatewayConfig.getPrivateAddress();
        }
        return String.format("https://%s:%d", ipAddresss, stack.getGatewayport());
    }

}
