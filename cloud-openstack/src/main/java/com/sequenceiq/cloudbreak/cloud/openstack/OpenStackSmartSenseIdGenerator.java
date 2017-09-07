package com.sequenceiq.cloudbreak.cloud.openstack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenStackSmartSenseIdGenerator {

    @Value("${cb.smartsense.configure:false}")
    private boolean configureSmartSense;

    @Value("${cb.smartsense.id.pattern:}")
    private String smartSenseIdPattern;

    public String getSmartSenseId() {
        String result = "";
        if (configureSmartSense) {
            result = String.format(smartSenseIdPattern, "0000", "00000000");
        }
        return result;
    }
}
