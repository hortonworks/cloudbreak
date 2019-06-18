package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ccm.endpoint.DirectServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientFactory.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Autowired(required = false)
    private ServiceEndpointFinder serviceEndpointFinder = new DirectServiceEndpointFinder();

    public FreeIpaClient getFreeIpaClientForStackId(Long stackId) throws Exception {
        LOGGER.debug("Retrieving stack for stack id {}", stackId);

        Stack stack = stackService.getStackById(stackId);

        return getFreeIpaClientForStack(stack);
    }

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws Exception {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getId());

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        GatewayConfig gatewayEndpointConfig;
        try {
            gatewayEndpointConfig = primaryGatewayConfig.getGatewayConfig(serviceEndpointFinder);
        } catch (ServiceEndpointLookupException | InterruptedException e) {
            throw new RuntimeException("Cannot determine service endpoint for gateway: " + primaryGatewayConfig, e);
        }
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(
                stack.getId(), gatewayEndpointConfig.getConnectionAddress());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);

        return new FreeIpaClientBuilder("admin", freeIpa.getAdminPassword(), freeIpa.getDomain().toUpperCase(),
                httpClientConfig, gatewayEndpointConfig.getGatewayPort().toString()).build();
    }
}
