package com.sequenceiq.freeipa.service.stack;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClientCertificate;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyRegistrationClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterServiceConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationRequest;
import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.clusterproxy.Tunnel;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.vault.FreeIpaCertVaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
public class ClusterProxyService {

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

    public ConfigRegistrationResponse registerFreeIpa(String accountId, String environmentCrn) {
        return registerFreeIpa(stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId));
    }

    public ConfigRegistrationResponse registerFreeIpa(Long stackId) {
        return registerFreeIpa(stackService.getStackById(stackId));
    }

    public ConfigRegistrationResponse registerFreeIpa(Stack stack) {
        LOGGER.debug("Registering freeipa with cluster-proxy: Environment CRN = [{}], Stack CRN = [{}]", stack.getEnvironmentCrn(), stack.getResourceCrn());

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(
                stack.getId(), primaryGatewayConfig.getGatewayUrl());

        GatewaySecreVaultRef gatewaySecretVaultRef = putGatewaySecretInVault(stack, primaryGatewayConfig.getClientCert(), primaryGatewayConfig.getClientKey());

        ClusterServiceConfig serviceConfig = stack.getUseCcm() ?
                createServiceConfigWithTunnelEnabled(stack, httpClientConfig, primaryGatewayConfig, gatewaySecretVaultRef) :
                createServiceConfig(stack, httpClientConfig, gatewaySecretVaultRef);

        List<ClusterServiceConfig> serviceConfigs = List.of(serviceConfig);
        LOGGER.debug("Registering service configs [{}]", serviceConfigs);
        ConfigRegistrationRequest request = new ConfigRegistrationRequest(stack.getResourceCrn(), List.of(), serviceConfigs, null);
        ConfigRegistrationResponse response = clusterProxyRegistrationClient.registerConfig(request);

        stackUpdater.updateClusterProxyRegisteredFlag(stack, true);

        return response;
    }

    public void deregisterFreeIpa(String accountId, String environmentCrn) {
        deregisterFreeIpa(stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId));
    }

    public void deregisterFreeIpa(Long stackId) {
        deregisterFreeIpa(stackService.getStackById(stackId));
    }

    public void deregisterFreeIpa(Stack stack) {
        LOGGER.debug("Deregistering freeipa with cluster-proxy: Environment CRN = [{}], Stack CRN = [{}]", stack.getEnvironmentCrn(), stack.getResourceCrn());
        stackUpdater.updateClusterProxyRegisteredFlag(stack, false);
        clusterProxyRegistrationClient.deregisterConfig(stack.getResourceCrn());
        LOGGER.debug("Cleaning up vault secrets for cluster-proxy");
        freeIpaCertVaultComponent.cleanupSecrets(stack);
    }

    private GatewaySecreVaultRef putGatewaySecretInVault(Stack stack, String clientCert, String clientKey) {
        LOGGER.debug("Putting vault secret for cluster-proxy");
        SecretResponse clientCertificateSecret =
                freeIpaCertVaultComponent.putGatewayClientCertificate(stack, clientCert);
        SecretResponse clientKeySecret =
                freeIpaCertVaultComponent.putGatewayClientKey(stack, clientKey);
        String keyRef = clientKeySecret.getSecretPath() + VAULT_KEY_SUFFIX;
        String secretRef = clientCertificateSecret.getSecretPath() + VAULT_KEY_SUFFIX;
        return new GatewaySecreVaultRef(keyRef, secretRef);
    }

    private ClusterServiceConfig createServiceConfig(Stack stack, HttpClientConfig httpClientConfig, GatewaySecreVaultRef gatewaySecretVaultRef) {
        return new ClusterServiceConfig(ClusterProxyConfiguration.FREEIPA_SERVICE_NAME,
                List.of(httpClientConfig.getApiAddress()),
                List.of(),
                new ClientCertificate(gatewaySecretVaultRef.keyRef, gatewaySecretVaultRef.secretRef),
                NO_TLS_STRICT_CHECK
        );
    }

    private ClusterServiceConfig createServiceConfigWithTunnelEnabled(
            Stack stack, HttpClientConfig httpClientConfig, GatewayConfig primaryGatewayConfig, GatewaySecreVaultRef gatewaySecretVaultRef) {

        String tunnelKey = primaryGatewayConfig.getInstanceId();
        String tunnelHost = primaryGatewayConfig.getPrivateAddress();
        Integer tunnelPort = primaryGatewayConfig.getGatewayPort();

        Tunnel tunnel = new Tunnel(tunnelKey, GATEWAY_SERVICE_TYPE, tunnelHost, tunnelPort);

        return new ClusterServiceConfig(ClusterProxyConfiguration.FREEIPA_SERVICE_NAME,
                List.of(httpClientConfig.getApiAddress()),
                List.of(),
                new ClientCertificate(gatewaySecretVaultRef.keyRef, gatewaySecretVaultRef.secretRef),
                NO_TLS_STRICT_CHECK,
                USE_TUNNEL,
                List.of(tunnel),
                stack.getAccountId()
        );
    }

    private static class GatewaySecreVaultRef {

        private final String keyRef;

        private final String secretRef;

        GatewaySecreVaultRef(String keyRef, String secretRef) {
            this.keyRef = keyRef;
            this.secretRef = secretRef;
        }
    }

}
