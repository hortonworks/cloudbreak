package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.test.ClientTestV1Endpoint;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
@Controller
public class ClientTestV1Controller implements ClientTestV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTestV1Controller.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Override
    public String userShow(Long id, String name) {
        FreeIpaClient freeIpaClient;
        try {
            freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackId(id);
        } catch (Exception e) {
            LOGGER.error("Error creating FreeIpaClient", e);
            return "FAILED TO CREATE CLIENT";
        }

        try {
            User user = freeIpaClient.userShow(name);
            LOGGER.info("Groups: {}", user.getMemberOfGroup());
            LOGGER.info("Success: {}", user);
        } catch (Exception e) {
            LOGGER.error("Error showing user {}", name, e);
            return "FAILED TO SHOW USER";
        }
        return "END";
    }
}
