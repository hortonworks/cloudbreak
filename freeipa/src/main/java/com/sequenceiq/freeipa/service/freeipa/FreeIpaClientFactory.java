package com.sequenceiq.freeipa.service.freeipa;

import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.freeipa.service.config.FmsClusterProxyEnablement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientFactory.class);

    private static final String ADMIN_USER = "admin";

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADERS =  Map.of("Proxy-Ignore-Auth", "true");

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private FmsClusterProxyEnablement fmsClusterProxyEnablement;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public FreeIpaClient getFreeIpaClientForStackId(Long stackId) throws Exception {
        LOGGER.debug("Retrieving stack for stack id {}", stackId);

        Stack stack = stackService.getStackById(stackId);

        return getFreeIpaClientForStack(stack);
    }

    public FreeIpaClient getFreeIpaClientByAccountAndEnvironment(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return getFreeIpaClientForStack(stack);
    }

    private String toClusterProxyBasepath(String freeIpaClusterCrn) {
        return String.format("/cluster-proxy/proxy/%s/%s/ipa", freeIpaClusterCrn, ClusterProxyConfiguration.FREEIPA_SERVICE_NAME);
    }

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getResourceCrn());

        try {
            if (fmsClusterProxyEnablement.isEnabled(stack) && Boolean.TRUE.equals(stack.getClusterProxyRegistered())) {
                HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
                FreeIpa freeIpa = freeIpaService.findByStack(stack);
                String clusterProxyPath = toClusterProxyBasepath(stack.getResourceCrn());

                return new FreeIpaClientBuilder(ADMIN_USER,
                    freeIpa.getAdminPassword(),
                    freeIpa.getDomain().toUpperCase(),
                    httpClientConfig,
                    clusterProxyConfiguration.getClusterProxyPort(),
                    clusterProxyPath,
                    ADDITIONAL_CLUSTER_PROXY_HEADERS).build();
            } else {
                GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
                HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(
                        stack.getId(), gatewayConfig.getPublicAddress());
                FreeIpa freeIpa = freeIpaService.findByStack(stack);
                return new FreeIpaClientBuilder(ADMIN_USER, freeIpa.getAdminPassword(), freeIpa.getDomain().toUpperCase(),
                        httpClientConfig, stack.getGatewayport()).build();
            }
        } catch (Exception e) {
            throw new FreeIpaClientException("Couldn't build FreeIPA client. "
                    + "Check if the FreeIPA security rules have not changed and the instance is in running state. " + e.getLocalizedMessage(), e);
        }
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }
}