package com.sequenceiq.cloudbreak.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.crypto.provider.OpenSSLJniProvider;

public class OpenSSLLoaderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenSSLLoaderUtil.class);

    private OpenSSLLoaderUtil() {
    }

    public static void registerOpenSSLJniProvider() {
        LOGGER.info("Registering OpenSSLJniProvider");
        OpenSSLJniProvider.register();
    }
}
