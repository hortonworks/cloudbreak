package com.sequenceiq.freeipa.util;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class SaltBootstrapVersionChecker extends AvailabilityChecker {

    public static final String SALT_BOOTSTRAP_PACKACE = "salt-bootstrap";

    private static final Versioned CHANGE_SALTUSER_PASSWORD_SUPPORT_AFTER_VERSION = () -> "0.13.5";

    public boolean isChangeSaltuserPasswordSupported(Stack stack) {
        return isPackageAvailable(stack, SALT_BOOTSTRAP_PACKACE, CHANGE_SALTUSER_PASSWORD_SUPPORT_AFTER_VERSION);
    }
}
