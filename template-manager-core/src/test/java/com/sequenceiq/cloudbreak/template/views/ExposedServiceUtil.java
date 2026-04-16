package com.sequenceiq.cloudbreak.template.views;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.service.ExposedService;

public class ExposedServiceUtil {

    private ExposedServiceUtil() {
    }

    public static ExposedService exposedService(String name) {
        ExposedService exposedService = new ExposedService(
                name,
                name,
                name,
                name,
                "/impala",
                name,
                true,
                1,
                80,
                true,
                true,
                true,
                true,
                true,
                "",
                "",
                "",
                true,
                "",
                Set.of()
        );
        return exposedService;
    }
}
