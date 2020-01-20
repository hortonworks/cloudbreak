package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.api.service.ExposedService;

public class ExposedServiceUtil {

    private ExposedServiceUtil() {
    }

    public static ExposedService exposedService(String name) {
        ExposedService exposedService = new ExposedService();
        exposedService.setApiIncluded(true);
        exposedService.setApiOnly(true);
        exposedService.setDisplayName(name);
        exposedService.setKnoxService(name);
        exposedService.setKnoxUrl(name);
        exposedService.setName(name);
        exposedService.setPort(1);
        exposedService.setServiceName(name);
        exposedService.setSsoSupported(true);
        exposedService.setTlsPort(1);
        exposedService.setVisible(true);
        return exposedService;
    }
}
