package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import com.sequenceiq.cloudbreak.api.service.ExposedService;

public class ExposedServiceUtil {

    private ExposedServiceUtil() {
    }

    public static ExposedService exposedService(String name) {
        ExposedService exposedService = ExposedService.builder()
                .withApiIncluded(true)
                .withApiOnly(true)
                .withDisplayName(name)
                .withKnoxService(name)
                .withKnoxUrl(name)
                .withName(name)
                .withTlsPort(1)
                .withPort(1)
                .withServiceName(name)
                .withSsoSupported(true)
                .withVisibleForDatalake(true)
                .withVisibleForDatahub(true)
                .build();
        return exposedService;
    }
}
