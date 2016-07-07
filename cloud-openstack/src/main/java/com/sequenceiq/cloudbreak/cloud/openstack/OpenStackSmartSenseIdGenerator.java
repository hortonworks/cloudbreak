package com.sequenceiq.cloudbreak.cloud.openstack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenStackSmartSenseIdGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackSmartSenseIdGenerator.class);

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
