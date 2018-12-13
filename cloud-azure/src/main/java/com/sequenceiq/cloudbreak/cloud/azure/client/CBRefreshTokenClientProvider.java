package com.sequenceiq.cloudbreak.cloud.azure.client;

import org.springframework.stereotype.Component;

@Component
public class CBRefreshTokenClientProvider {

    public CBRefreshTokenClient getCBRefreshTokenClient(String baseUrl) {
        return new CBRefreshTokenClient(baseUrl);
    }

}
