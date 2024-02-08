package com.sequenceiq.freeipa.service.image.userdata;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("cb")
@Component
public class UserDataBuilderParams {

    private String customUserData = "touch /tmp/cb-custom-data-default.txt";

    private Map<String, String> userDataSecrets;

    public String getCustomUserData() {
        return customUserData;
    }

    public void setCustomUserData(String customUserData) {
        this.customUserData = customUserData;
    }

    public Map<String, String> getUserDataSecrets() {
        return userDataSecrets;
    }

    public void setUserDataSecrets(Map<String, String> userDataSecrets) {
        this.userDataSecrets = userDataSecrets;
    }
}
