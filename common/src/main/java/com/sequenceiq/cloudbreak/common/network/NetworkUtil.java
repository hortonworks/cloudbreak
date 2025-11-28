package com.sequenceiq.cloudbreak.common.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtil.class);

    private NetworkUtil() {
    }

    public static Optional<String> resolveHostAddress(String hostAddress) {
        try {
            LOGGER.debug("Trying to resolve host address {}", hostAddress);
            return StringUtils.isNotBlank(hostAddress) ? Optional.of(InetAddress.getByName(hostAddress).getHostAddress()) : Optional.empty();
        } catch (UnknownHostException | RuntimeException e) {
            LOGGER.warn("Failed to resolve host address {}", hostAddress, e);
            return Optional.empty();
        }
    }
}
