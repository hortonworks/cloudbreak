package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;

@Component
class FreeIpaServerRequestProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandler.class);

    private static final String FREEIPA_HOSTNAME = "ipaserver";

    FreeIpaServerRequest create(EnvironmentDto environment) {
        String domain = environment.getDomain();
        LOGGER.info("Creating FreeIPA request for environment: '{}' with domain: '{}'", environment.getName(), domain);

        String adminGroupName = environment.getAdminGroupName();
        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setAdminPassword(FreeIpaPasswordUtil.generatePassword());
        freeIpaServerRequest.setDomain(domain);
        freeIpaServerRequest.setHostname(FREEIPA_HOSTNAME);
        freeIpaServerRequest.setAdminGroupName(adminGroupName);
        LOGGER.info("FreeIpaServerRequest created for environment: {}, request {}", environment.getName(), freeIpaServerRequest);
        return freeIpaServerRequest;
    }
}
