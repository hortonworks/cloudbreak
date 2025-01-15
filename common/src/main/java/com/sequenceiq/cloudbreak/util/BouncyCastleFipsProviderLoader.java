package com.sequenceiq.cloudbreak.util;

import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BouncyCastleFipsProviderLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(BouncyCastleFipsProviderLoader.class);

    private static final String BC_FIPS_PROVIDER_NAME = "BCFIPS";

    private BouncyCastleFipsProviderLoader() {
    }

    public static void load() {
        Provider bcProvider = Security.getProvider(BC_FIPS_PROVIDER_NAME);
        if (bcProvider != null) {
            LOGGER.info("{} security provider already added nothing to do.", BC_FIPS_PROVIDER_NAME);
        } else {
            try {
                LOGGER.info("{} security provider hasn't been added, trying to adding it. This is mainly for local development", BC_FIPS_PROVIDER_NAME);
                Security.addProvider(new BouncyCastleFipsProvider());
                LOGGER.info("{} security provider has been added successfully", BC_FIPS_PROVIDER_NAME);
            } catch (Exception e) {
                LOGGER.warn("{} security provider could not be loaded", BC_FIPS_PROVIDER_NAME, e);
            }
        }
    }
}
