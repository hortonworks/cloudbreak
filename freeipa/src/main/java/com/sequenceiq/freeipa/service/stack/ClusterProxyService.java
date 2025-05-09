package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_BACKEND_ID_FORMAT;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceFamilies;
import com.sequenceiq.cloudbreak.clusterproxy.CcmV2Config;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyEnablementService;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequestBuilder;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.clusterproxy.TunnelEntry;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultConfigException;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.util.ClusterProxyServiceAvailabilityChecker;
import com.sequenceiq.freeipa.util.HealthCheckAvailabilityChecker;
import com.sequenceiq.freeipa.vault.FreeIpaCertVaultComponent;

@Service
public class ClusterProxyService {

    private static final String FREEIPA_SERVICE_NAME = "freeipa";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyService.class);

    private static final String GATEWAY_SERVICE_TYPE = "GATEWAY";

    private static final String NGINX_PROTOCOL = "https";

    @Value("${clusterProxy.healthCheckV1.intervalInSec}")
    private int intervalInSecV1;

    @Value("${clusterProxy.healthCheckV2.intervalInSec}")
    private int intervalInSecV2;

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterProxyRegistrationClient clusterProxyRegistrationClient;

    @Inject
    private FreeIpaCertVaultComponent freeIpaCertVaultComponent;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClusterProxyEnablementService clusterProxyEnablementService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private HealthCheckAvailabilityChecker healthCheckAvailabilityChecker;

    @Inject
    private ClusterProxyServiceAvailabilityChecker clusterProxyServiceAvailabilityChecker;

    public Optional<ConfigRegistrationResponse> registerFreeIpa(String accountId, String environmentCrn) {
        return registerFreeIpa(stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId), null, false);
    }

    public Optional<ConfigRegistrationResponse> registerFreeIpa(Long stackId) {
        return registerFreeIpa(stackService.getStackById(stackId), null, false);
    }

    public Optional<ConfigRegistrationResponse> updateFreeIpaRegistrationAndWait(Long stackId, List<String> instanceIdsToRegister) {
        return registerFreeIpa(stackService.getStackById(stackId), instanceIdsToRegister, false);
    }

    public Optional<ConfigRegistrationResponse> registerFreeIpaForBootstrap(Long stackId) {
        return registerFreeIpa(stackService.getStackById(stackId), null, true);
    }

    private Optional<ConfigRegistrationResponse> registerFreeIpa(Stack stack, List<String> instanceIdsToRegister, boolean bootstrap) {
        MDCBuilder.buildMdcContext(stack);
        if (!clusterProxyEnablementService.isClusterProxyApplicable(stack.getCloudPlatform())) {
            LOGGER.debug("Cluster Proxy integration disabled. Skipping registering FreeIpa [{}]", stack);
            return Optional.empty();
        }

        LOGGER.debug("Registering freeipa with cluster-proxy: Environment CRN = [{}], Stack CRN = [{}], bootstrap: [{}]",
                stack.getEnvironmentCrn(), stack.getResourceCrn(), bootstrap);

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        ClientCertificate clientCertificate = clientCertificates(stack);

        boolean preferPrivateIp = stack.getTunnel().useCcm();
        List<GatewayConfig> tunnelGatewayConfigs;
        List<ClusterServiceConfig> serviceConfigs = new LinkedList<>();
        serviceConfigs.add(createServiceConfig(stack, FREEIPA_SERVICE_NAME, primaryGatewayConfig, clientCertificate, preferPrivateIp));

        if (bootstrap) {
            tunnelGatewayConfigs = List.of(primaryGatewayConfig);
        } else if (clusterProxyServiceAvailabilityChecker.isDnsBasedServiceNameAvailable(stack)) {
            List<GatewayConfig> targetGatewayConfigs = gatewayConfigs.stream()
                    .filter(gatewayConfig -> Objects.nonNull(gatewayConfig.getInstanceId()))
                    .filter(gatewayConfig -> Objects.isNull(instanceIdsToRegister) || instanceIdsToRegister.contains(gatewayConfig.getInstanceId()))
                    .collect(Collectors.toList());
            serviceConfigs.addAll(createDnsMappedServiceConfigs(stack, targetGatewayConfigs, clientCertificate, preferPrivateIp));
            tunnelGatewayConfigs = targetGatewayConfigs;
        } else {
            tunnelGatewayConfigs = List.of(primaryGatewayConfig);
        }

        ConfigRegistrationRequestBuilder requestBuilder = new ConfigRegistrationRequestBuilder(stack.getResourceCrn())
                .withServices(serviceConfigs)
                .withAccountId(stack.getAccountId());
        if (stack.getTunnel().useCcmV1()) {
            requestBuilder.withTunnelEntries(createTunnelEntries(stack, tunnelGatewayConfigs));
        } else if (stack.getTunnel().useCcmV2OrJumpgate()) {
            requestBuilder.withEnvironmentCrn(stack.getEnvironmentCrn());
            requestBuilder.withCcmV2Entries(createCcmV2Configs(stack, tunnelGatewayConfigs));
        }
        ConfigRegistrationRequest request = requestBuilder.build();
        LOGGER.debug("Registering cluster proxy configuration [{}]", request);
        ConfigRegistrationResponse response = clusterProxyRegistrationClient.registerConfig(request);

        stackUpdater.updateClusterProxyRegisteredFlag(stack, true);

        return Optional.of(response);
    }

    public boolean useClusterProxyForCommunication(Tunnel tunnel, String cloudPlatform) {
        return clusterProxyEnablementService.isClusterProxyApplicable(cloudPlatform) && tunnel.useClusterProxy();
    }

    public boolean useClusterProxyForCommunication(Stack stack) {
        return useClusterProxyForCommunication(stack.getTunnel(), stack.getCloudPlatform());
    }

    public boolean isCreateConfigForClusterProxy(Stack stack) {
        return useClusterProxyForCommunication(stack) && stack.getClusterProxyRegistered();
    }

    public void deregisterFreeIpa(String accountId, String environmentCrn) {
        deregisterFreeIpa(stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId));
    }

    public void deregisterFreeIpa(Long stackId) {
        deregisterFreeIpa(stackService.getStackById(stackId));
    }

    private void deregisterFreeIpa(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (!clusterProxyEnablementService.isClusterProxyApplicable(stack.getCloudPlatform())) {
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
            boolean preferPrivateIp) {
        return new ClusterServiceConfig(serviceName,
                List.of(getNginxEndpointForRegistration(stack, gatewayConfig, preferPrivateIp)),
                List.of(),
                clientCertificate
        );
    }

    private List<ClusterServiceConfig> createDnsMappedServiceConfigs(Stack stack, List<GatewayConfig> gatewayConfigs, ClientCertificate clientCertificate,
            boolean preferPrivateIp) {
        return gatewayConfigs.stream()
                .map(gatewayConfig -> createServiceConfig(stack, gatewayConfig.getHostname(), gatewayConfig, clientCertificate, preferPrivateIp))
                .collect(Collectors.toList());
    }

    private int getIntervalInSec(Stack stack) {
        if (healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(stack)) {
            return intervalInSecV2;
        }
        return intervalInSecV1;
    }

    private List<CcmV2Config> createCcmV2Configs(Stack stack, List<GatewayConfig> gatewayConfigs) {
        return gatewayConfigs.stream()
                .map(gatewayConfig -> new CcmV2Config(
                        stack.getCcmV2AgentCrn(),
                        gatewayConfig.getPrivateAddress(),
                        getNginxPort(stack),
                        String.format(CCMV2_BACKEND_ID_FORMAT, stack.getCcmV2AgentCrn(), gatewayConfig.getInstanceId()),
                        FREEIPA_SERVICE_NAME
                ))
                .collect(Collectors.toList());
    }

    private List<TunnelEntry> createTunnelEntries(Stack stack, List<GatewayConfig> gatewayConfigs) {
        return gatewayConfigs.stream()
                .map(gatewayConfig -> new TunnelEntry(
                        gatewayConfig.getInstanceId(),
                        GATEWAY_SERVICE_TYPE,
                        gatewayConfig.getPrivateAddress(),
                        getNginxPort(stack),
                        stack.getMinaSshdServiceId()))
                .collect(Collectors.toList());
    }

    public String getProxyPath(String crn) {
        return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), crn, FREEIPA_SERVICE_NAME);
    }

    public String getProxyPathPgwAsFallBack(Stack stack, String serviceName) {
        return getProxyPath(stack, serviceName, FREEIPA_SERVICE_NAME);
    }

    private String getProxyPath(Stack stack, String serviceName, String defaultServiceName) {
        Optional<String> registeredServiceName = getRegisteredServiceNameOrEmpty(stack.getResourceCrn(), serviceName);
        String proxyServiceName = registeredServiceName.orElse(defaultServiceName);
        LOGGER.info("Service name used for connection: [{}]", proxyServiceName);
        return String.format("%s/proxy/%s/%s", clusterProxyConfiguration.getClusterProxyBasePath(), stack.getResourceCrn(), proxyServiceName);
    }

    private Optional<String> getRegisteredServiceNameOrEmpty(String crn, String serviceName) {
        if (StringUtils.isNotBlank(serviceName) && isServiceEndpointWithIdentityRegistered(crn, serviceName)) {
            LOGGER.info("ServiceName [{}] is registered", serviceName);
            return Optional.of(serviceName);
        } else {
            LOGGER.info("ServiceName [{}] is not registered or not defined", serviceName);
            return Optional.empty();
        }
    }

    private boolean isServiceEndpointWithIdentityRegistered(String crn, String serviceName) {
        ReadConfigResponse readConfigResponse = clusterProxyRegistrationClient.readConfig(crn);
        LOGGER.debug("Check if internal endpoint with serviceName [{}] is registered", serviceName);
        return readConfigResponse.getServices() != null && readConfigResponse.getServices().stream()
                .anyMatch(readConfigService -> serviceName.equals(readConfigService.getName()));
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
            String clientKeyRef = vaultPath(securityConfig.getClientKeyVaultSecret());
            clientCertificate = new ClientCertificate(clientKeyRef, clientCertRef);
        }
        return clientCertificate;
    }

    private String getNginxEndpointForRegistration(Stack stack, GatewayConfig gatewayConfig, boolean preferPrivateIp) {
        String ipAddress = preferPrivateIp ? gatewayConfig.getPrivateAddress() : gatewayConfig.getPublicAddress();
        return String.format("%s://%s:%d", NGINX_PROTOCOL, ipAddress, getNginxPort(stack));
    }

    private int getNginxPort(Stack stack) {
        return Optional.ofNullable(stack.getGatewayport()).orElse(ServiceFamilies.GATEWAY.getDefaultPort());
    }

}
