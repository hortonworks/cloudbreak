package com.sequenceiq.freeipa.service.freeipa.host;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class MaxHostnameLengthPolicyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxHostnameLengthPolicyService.class);

    private static final int MAX_HOSTNAME_LENGTH_WITH_DELIMITER = 64;

    private static final int MAX_NAME_LENGTH = 255;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private RhelClientHelper rhelClientHelper;

    public void updateMaxHostnameLength(Stack stack, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        Config ipaConfig = freeIpaClient.getConfig();
        Integer maxHostNameLength = calculateRquiredMaxHostnameLength(stack);
        LOGGER.debug("The maximum hostname length that is required by the environment: '{}', the maximum that is configured for IPA: '{}'", maxHostNameLength,
                ipaConfig.getIpamaxhostnamelength());
        if (ipaConfig.getIpamaxhostnamelength() != null && ipaConfig.getIpamaxhostnamelength() < maxHostNameLength) {
            boolean clientConnectedToRhel = rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient);
            if (clientConnectedToRhel) {
                LOGGER.info("Set maximum hostname length to '{}'", maxHostNameLength);
                freeIpaClient.setMaxHostNameLength(maxHostNameLength);
            } else {
                Optional<String> rhelInstance = rhelClientHelper.findRhelInstance(stack);
                if (rhelInstance.isPresent()) {
                    FreeIpaClient rhelFreeIpaClient = freeIpaClientFactory.getFreeIpaClientForInstance(stack, rhelInstance.get());
                    LOGGER.info("Set maximum hostname length to '{}' on host: {}", maxHostNameLength, rhelInstance.get());
                    rhelFreeIpaClient.setMaxHostNameLength(maxHostNameLength);
                } else {
                    LOGGER.warn("Couldn't found RHEL FreeIPA instance to set maximum hostname length");
                }
            }
        }
    }

    private Integer calculateRquiredMaxHostnameLength(Stack stack) {
        if (rhelClientHelper.isClientConnectedToSpecificOs(stack, OsType.RHEL9)) {
            return MAX_NAME_LENGTH;
        }
        int domainLength = stack.getPrimaryGateway().map(im -> im.getDomain().length()).orElse(MAX_NAME_LENGTH);
        return Math.min(domainLength + MAX_HOSTNAME_LENGTH_WITH_DELIMITER, MAX_NAME_LENGTH);
    }
}
