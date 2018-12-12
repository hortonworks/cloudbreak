package com.sequenceiq.cloudbreak.cloud.azure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CBRefreshTokenClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CBRefreshTokenClientProvider.class);

    public CBRefreshTokenClient getCBRefreshTokenClient(String baseUrl) {
        return new CBRefreshTokenClient(baseUrl);
    }

}
