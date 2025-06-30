package com.sequenceiq.freeipa.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class SaltBootstrapVersionChecker extends AvailabilityChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltBootstrapVersionChecker.class);

    private static final String SALT_BOOTSTRAP_PACKACE = "salt-bootstrap";

    private static final Versioned CHANGE_SALTUSER_PASSWORD_SUPPORT_AFTER_VERSION = () -> "0.13.5";

    private static final Versioned FQDN_AS_HOSTNAME_SUPPORT_AFTER_VERSION = () -> "0.14.3";

    public boolean isChangeSaltuserPasswordSupported(Stack stack) {
        return isPackageAvailable(stack, SALT_BOOTSTRAP_PACKACE, CHANGE_SALTUSER_PASSWORD_SUPPORT_AFTER_VERSION);
    }

    public boolean isFqdnAsHostnameSupported(Stack stack) {
        LOGGER.debug("Checking if FQDN as hostname is supported");
        return isPackageAvailable(stack, SALT_BOOTSTRAP_PACKACE, FQDN_AS_HOSTNAME_SUPPORT_AFTER_VERSION)
                && doesAllImageSupport(stack, SALT_BOOTSTRAP_PACKACE, FQDN_AS_HOSTNAME_SUPPORT_AFTER_VERSION);
    }
}
