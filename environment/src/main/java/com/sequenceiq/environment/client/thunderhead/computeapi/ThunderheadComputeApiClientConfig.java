package com.sequenceiq.environment.client.thunderhead.computeapi;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public record ThunderheadComputeApiClientConfig(
        @Value("${environment.thunderhead.computeapi.host:}") String host,
        @Value("${environment.thunderhead.computeapi.port:}") int port,
        @Value("${environment.thunderhead.computeapi.apiBasePath:}") String apiBasePath) {

    public String getClientConnectionUrl() {
        String wrappedApiBasePath = StringUtils.defaultIfEmpty(apiBasePath, "");
        return String.format("%s%s:%d%s", "http://", host, port, wrappedApiBasePath);
    }
}
