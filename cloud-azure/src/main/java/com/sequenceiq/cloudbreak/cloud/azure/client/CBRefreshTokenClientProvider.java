package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.net.Proxy;

import org.springframework.stereotype.Component;

@Component
public class CBRefreshTokenClientProvider {

    public CBRefreshTokenClient getCBRefreshTokenClient(String baseUrl, Proxy proxy) {
        return new CBRefreshTokenClient(baseUrl, proxy);
    }

}
