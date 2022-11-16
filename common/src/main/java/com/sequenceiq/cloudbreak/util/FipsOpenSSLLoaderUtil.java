package com.sequenceiq.cloudbreak.util;

import java.security.Provider;
import java.security.Security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.crypto.provider.OpenSSLJniProvider;

public class FipsOpenSSLLoaderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FipsOpenSSLLoaderUtil.class);

    // CHECKSTYLE:OFF
    /**
     * Based on FIPS default configuration for OpenJDK11 the following provider should exist in FIPS environments
     * https://access.redhat.com/documentation/en-us/openjdk/11/html-single/configuring_openjdk_11_on_rhel_with_fips/index#ref_openjdk-default-fips-configuration_openjdk
     */
    // CHECKSTYLE:ON
    private static final String SUN_PKCS_11_NSS_FIPS_PROVIDER_NAME = "SunPKCS11-NSS-FIPS";

    private FipsOpenSSLLoaderUtil() {
    }

    public static void registerOpenSSLJniProvider() {
        Provider sunNSSFipsProvider = Security.getProvider(SUN_PKCS_11_NSS_FIPS_PROVIDER_NAME);
        if (sunNSSFipsProvider != null) {
            LOGGER.info("Registering OpenSSLJniProvider, because FIPS related security provider '{}' is available.", SUN_PKCS_11_NSS_FIPS_PROVIDER_NAME);
            OpenSSLJniProvider.register();
        } else {
            LOGGER.info("OpenSSLJniProvider doesn't need to be registered because '{}' is NOT available.", SUN_PKCS_11_NSS_FIPS_PROVIDER_NAME);
        }
    }
}
