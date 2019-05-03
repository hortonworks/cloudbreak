package com.sequenceiq.freeipa.service.image;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("cb")
@Component
public class UserDataBuilderParams {

    private String customData = "touch /tmp/cb-custom-data-default.txt";

    public String getCustomData() {
        return customData;
    }

    public void setCustomData(String customData) {
        this.customData = customData;
    }

}
