package com.sequenceiq.cloudbreak.service.aws;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class Ec2UserDataBuilder {

    private String ec2userDataScript;

    @PostConstruct
    public void readUserDataScript() throws IOException {
        ec2userDataScript = FileReaderUtils.readFileFromClasspath("ec2-init.sh");
    }

    public String buildUserData(Map<String, String> parameters) {
        StringBuilder stringBuilder = new StringBuilder("#!/bin/bash\n");
        stringBuilder.append("set -x\n").append("\n");
        for (Entry<String, String> parameter : parameters.entrySet()) {
            stringBuilder.append(parameter.getKey()).append("=").append(parameter.getValue()).append("\n");
        }
        stringBuilder.append("\n").append(ec2userDataScript);
        return stringBuilder.toString();
    }

    void setEc2userDataScript(String ec2userDataScript) {
        this.ec2userDataScript = ec2userDataScript;
    }

}
