package com.sequenceiq.cloudbreak.service.sslcontext;

import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.KeyStoreUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Service
public class SSLContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSLContextProvider.class);

    @Cacheable(cacheNames = "sslContextCache", key = "{ #serverCert, #additionalServerCert, #clientCert, #clientKey }")
    public SSLContext getSSLContext(String serverCert, Optional<String> additionalServerCert, String clientCert, String clientKey) {
        try {
            LOGGER.info("Generating custom SSLContext");
            return SSLContexts.custom()
                    .loadTrustMaterial(KeyStoreUtil.createTrustStore(serverCert, additionalServerCert), null)
                    .loadKeyMaterial(KeyStoreUtil.createKeyStore(clientCert, clientKey), "consul".toCharArray())
                    .build();
        } catch (Exception e) {
            LOGGER.info("Cannot create SSL context", e);
            throw new CloudbreakServiceException("Cannot create SSL context", e);
        }
    }
}
