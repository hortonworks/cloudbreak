package com.sequenceiq.freeipa.service.freeipa.host;

import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class MaxHostnameLengthPolicyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxHostnameLengthPolicyService.class);

    private static final int MAX_HOSTNAME_LENGTH_WITH_DELIMITER = 64;

    private static final int MAX_NAME_LENGTH = 255;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public void updateMaxHostnameLength(Stack stack, FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        Config ipaConfig = freeIpaClient.getConfig();
        Integer maxHostNameLength = calculateRquiredMaxHostnameLength(stack);
        LOGGER.debug("The maximum hostname length that is required by the environment: '{}', the maximum that is configured for IPA: '{}'", maxHostNameLength,
                ipaConfig.getIpamaxhostnamelength());
        if (ipaConfig.getIpamaxhostnamelength() != null && ipaConfig.getIpamaxhostnamelength() < maxHostNameLength) {
            boolean clientConnectedToRhel8 = isClientConnectedToRhel8(stack, freeIpaClient);
            if (clientConnectedToRhel8) {
                LOGGER.info("Set maximum hostname length to '{}'", maxHostNameLength);
                freeIpaClient.setMaxHostNameLength(maxHostNameLength);
            } else {
                Optional<String> rhel8Instance = findRhel8Instance(stack);
                if (rhel8Instance.isPresent()) {
                    FreeIpaClient rhel8FreeIpaClient = freeIpaClientFactory.getFreeIpaClientForInstance(stack, rhel8Instance.get());
                    LOGGER.info("Set maximum hostname length to '{}' on host: {}", maxHostNameLength, rhel8Instance.get());
                    rhel8FreeIpaClient.setMaxHostNameLength(maxHostNameLength);
                } else {
                    LOGGER.warn("Couldn't found RHEL8 FreeIPA instance to set maximum hostname length");
                }
            }
        }
    }

    private Optional<String> findRhel8Instance(Stack stack) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> {
                    Image image = im.getImage().getSilent(Image.class);
                    return OsType.RHEL8.getOsType().equalsIgnoreCase(image.getOsType());
                })
                .map(InstanceMetaData::getDiscoveryFQDN)
                .findFirst();
    }

    private boolean isClientConnectedToRhel8(Stack stack, FreeIpaClient freeIpaClient) {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()))
                .filter(im -> im.getDiscoveryFQDN().equals(freeIpaClient.getHostname()))
                .map(InstanceMetaData::getImage)
                .filter(Objects::nonNull)
                .map(image -> image.getSilent(Image.class))
                .anyMatch(image -> OsType.RHEL8.getOsType().equalsIgnoreCase(image.getOsType()));
    }

    private Integer calculateRquiredMaxHostnameLength(Stack stack) {
        int domainLength = stack.getPrimaryGateway().map(im -> im.getDomain().length()).orElse(MAX_NAME_LENGTH);
        return Math.min(domainLength + MAX_HOSTNAME_LENGTH_WITH_DELIMITER, MAX_NAME_LENGTH);
    }
}
