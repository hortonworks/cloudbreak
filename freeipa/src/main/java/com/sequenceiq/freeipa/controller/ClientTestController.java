package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.model.endpoint.ClientTestEndpoint;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientBuilder;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Controller
public class ClientTestController implements ClientTestEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTestController.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Override
    public String userShow(Long id, String name) {
        Stack stack = stackService.getStackById(id);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(id, primaryGatewayConfig.getPublicAddress());
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        try {
            FreeIpaClient freeIpaClient =
                    new FreeIpaClientBuilder("admin", freeIpa.getAdminPassword(), freeIpa.getDomain().toUpperCase(), httpClientConfig,
                            stack.getGatewayport().toString()).build();
            RPCResponse<User> userShow = freeIpaClient.userShow(name, null);
            User user = userShow.getResult();
            LOGGER.info("Groups: {}", user.getMemberOfGroup());
            LOGGER.info("Success: {}", user);
        } catch (Exception e) {
            LOGGER.error("Test error", e);
        }
        return "END";
    }
}
