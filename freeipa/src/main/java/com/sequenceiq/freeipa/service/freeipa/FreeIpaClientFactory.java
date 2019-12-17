package com.sequenceiq.freeipa.service.freeipa;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.client.ClusterProxyErrorRpcListener;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientFactory.class);

    private static final String ADMIN_USER = "admin";

    private static final Map<String, String> ADDITIONAL_CLUSTER_PROXY_HEADERS = Map.of("Proxy-Ignore-Auth", "true");

    private static final JsonRpcClient.RequestListener CLUSTER_PROXY_ERROR_LISTENER = new ClusterProxyErrorRpcListener();

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public FreeIpaClient getFreeIpaClientForStackId(Long stackId) throws FreeIpaClientException {
        LOGGER.debug("Retrieving stack for stack id {}", stackId);

        Stack stack = stackService.getStackById(stackId);

        return getFreeIpaClientForStack(stack);
    }

    public FreeIpaClient getFreeIpaClientByAccountAndEnvironment(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        return getFreeIpaClientForStack(stack);
    }

    private String toClusterProxyBasepath(String freeIpaClusterCrn) {
        return String.format("%s%s", clusterProxyService.getProxyPath(freeIpaClusterCrn), FreeIpaClientBuilder.DEFAULT_BASE_PATH);
    }

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getResourceCrn());

        try {
            if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
                return getFreeIpaClientBuilderForClusterProxy(stack).build();
            } else {
                return getFreeIpaClientBuilder(stack).build();
            }
        } catch (Exception e) {
            throw new FreeIpaClientException("Couldn't build FreeIPA client. "
                    + "Check if the FreeIPA security rules have not changed and the instance is in running state. " + e.getLocalizedMessage(), e);
        }
    }

    public FreeIpaClient getFreeIpaClientForStackWithPing(Stack stack) throws Exception {
        LOGGER.debug("Ping the login endpoint and creating FreeIpaClient for stack {}", stack.getResourceCrn());
        if (clusterProxyService.isCreateConfigForClusterProxy(stack)) {
            return getFreeIpaClientBuilderForClusterProxy(stack).buildWithPing();
        } else {
            return getFreeIpaClientBuilder(stack).buildWithPing();
        }
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilderForClusterProxy(Stack stack) throws Exception {
        HttpClientConfig httpClientConfig = new HttpClientConfig(clusterProxyConfiguration.getClusterProxyHost());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        String clusterProxyPath = toClusterProxyBasepath(stack.getResourceCrn());

        return new FreeIpaClientBuilder(ADMIN_USER,
                freeIpa.getAdminPassword(),
                freeIpa.getDomain().toUpperCase(),
                httpClientConfig,
                clusterProxyConfiguration.getClusterProxyPort(),
                clusterProxyPath,
                ADDITIONAL_CLUSTER_PROXY_HEADERS,
                CLUSTER_PROXY_ERROR_LISTENER);
    }

    private FreeIpaClientBuilder getFreeIpaClientBuilder(Stack stack) throws Exception {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(
                stack, gatewayConfig.getPublicAddress());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        return new FreeIpaClientBuilder(ADMIN_USER, freeIpa.getAdminPassword(), freeIpa.getDomain().toUpperCase(),
                httpClientConfig, stack.getGatewayport());
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }
}