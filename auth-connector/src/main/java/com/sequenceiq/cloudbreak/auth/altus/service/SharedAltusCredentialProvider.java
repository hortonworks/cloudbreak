package com.sequenceiq.cloudbreak.auth.altus.service;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;

@Component
public class SharedAltusCredentialProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AltusIAMService.class);

    private final boolean globalUseSharedAltusCredential;

    private final String sharedAltusAccessKey;

    private final char[] sharedAltusSecretKey;

    public SharedAltusCredentialProvider(AltusDatabusConfiguration altusDatabusConfiguration) {
        this.globalUseSharedAltusCredential = altusDatabusConfiguration.isUseSharedAltusCredential();
        this.sharedAltusAccessKey = altusDatabusConfiguration.getSharedAccessKey();
        this.sharedAltusSecretKey = altusDatabusConfiguration.getSharedSecretKey();
    }

    public Optional<AltusCredential> getSharedCredentialIfConfigured(boolean useSharedCredential) {
        if (globalUseSharedAltusCredential) {
            LOGGER.debug("Use shared altus credential is turned on for generating altus credential and access keys.");
            if (areDatabusCredentialsFilled(useSharedCredential)) {
                LOGGER.debug("Access and secret keys are set manually application wide for Databus, skip machine user and access key generation");
                return Optional.of(new AltusCredential(sharedAltusAccessKey, sharedAltusSecretKey));
            } else {
                LOGGER.debug("Use shared credential global config credential is set, but no shared access/secret keypair is used.");
            }
        }
        return Optional.empty();
    }

    private boolean areDatabusCredentialsFilled(boolean useSharedCredential) {
        return useSharedCredential && sharedAltusSecretKey != null && sharedAltusSecretKey.length > 0 && StringUtils.isNotBlank(sharedAltusAccessKey);
    }

    public boolean isSharedAltusCredentialInUse(boolean useSharedCredential) {
        return useSharedCredential && areDatabusCredentialsFilled(globalUseSharedAltusCredential);
    }
}
