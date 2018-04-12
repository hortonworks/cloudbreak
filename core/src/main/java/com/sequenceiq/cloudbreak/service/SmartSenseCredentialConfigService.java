package com.sequenceiq.cloudbreak.service;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SmartSenseCredentialConfigService {

    @Value("${smartsense.upload.host:}")
    private String host;

    @Value("${smartsense.upload.username:}")
    private String username;

    @Value("${smartsense.upload.password:}")
    private String password;

    public boolean areCredentialsSpecified() {
        return StringUtils.isNoneBlank(host, username, password);
    }

    public Map<String, Object> getCredentials() {
        Map<String, Object> result = new HashMap<>();
        if (areCredentialsSpecified()) {
            Map<String, Object> credentials = ImmutableMap.of("host", host, "username", username, "password", password);
            result.put("smartsense_upload", credentials);
        }
        return result;
    }
}
