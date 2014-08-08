package com.sequenceiq.cloudbreak.service.stack.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class UserDataBuilder {

    @Value("${cb.host.addr}")
    private String hostAddress;

    private Map<CloudPlatform, String> userDataScripts = new HashMap<>();

    public void setUserDataScripts(Map<CloudPlatform, String> userDataScripts) {
        this.userDataScripts = userDataScripts;
    }

    @PostConstruct
    public void readUserDataScript() throws IOException {
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            userDataScripts.put(cloudPlatform, FileReaderUtils.readFileFromClasspath(String.format("%s-init.sh", cloudPlatform.getInitScriptPrefix())));
        }
    }

    public String build(CloudPlatform cloudPlatform, String metadataHash, Map<String, String> parameters) {
        parameters.put("METADATA_ADDRESS", hostAddress);
        parameters.put("METADATA_HASH", metadataHash);
        String ec2userDataScript = userDataScripts.get(cloudPlatform);
        StringBuilder stringBuilder = new StringBuilder("#!/bin/bash\n");
        //StringBuilder stringBuilder = new StringBuilder("");
        stringBuilder.append("set -x\n").append("\n");
        for (Entry<String, String> parameter : parameters.entrySet()) {
            stringBuilder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("\n");
        }
        stringBuilder.append("\n").append(ec2userDataScript);
        return stringBuilder.toString();
    }

    protected void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }
}
