package com.sequenceiq.freeipa.service.freeipa;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
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

    public FreeIpaClient getFreeIpaClientForStack(Stack stack) throws FreeIpaClientException {
        LOGGER.debug("Creating FreeIpaClient for stack {}", stack.getResourceCrn());

        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(
                stack.getId(), primaryGatewayConfig.getPublicAddress());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);

        try {
            return new FreeIpaClientBuilder(ADMIN_USER, freeIpa.getAdminPassword(), freeIpa.getDomain().toUpperCase(),
                    httpClientConfig, stack.getGatewayport().toString()).build();
        } catch (Exception e) {
            throw new FreeIpaClientException("Couldn't build FreeIPA client: " + e.getLocalizedMessage(), e);
        }
    }

    public String getAdminUser() {
        return ADMIN_USER;
    }
}