package com.sequenceiq.cloudbreak.service.stack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class UserDataBuilder {

    private Map<String, String> parameters = new HashMap<>();
    private CloudPlatform cloudPlatform;

    private UserDataBuilder() {

    }

    public static UserDataBuilder builder() {
        return new UserDataBuilder();
    }

    public UserDataBuilder withCloudPlatform(CloudPlatform cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
        return this;
    }

    public UserDataBuilder withEnvironmentVariable(String key, String value) {
        this.parameters.put(key, value);
        return this;
    }

    public UserDataBuilder withEnvironmentVariables(Map<String, String> entries) {
        this.parameters.putAll(entries);
        return this;
    }


    public String build() throws IOException {
        Validate.notNull(cloudPlatform);
        Validate.notEmpty(parameters);
        String ec2userDataScript = FileReaderUtils.readFileFromClasspath(String.format("%s-init.sh", cloudPlatform.getValue()));
        StringBuilder stringBuilder = new StringBuilder("#!/bin/bash\n");
        stringBuilder.append("set -x\n").append("\n");
        for (Entry<String, String> parameter : parameters.entrySet()) {
            stringBuilder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("\n");
        }
        stringBuilder.append("\n").append(ec2userDataScript);
        return stringBuilder.toString();
    }
}
