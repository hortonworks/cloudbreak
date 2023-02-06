package com.sequenceiq.cloudbreak.vm;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("vm")
public class VirtualMachineConfiguration {

    private List<Integer> supportedJavaVersions;

    public List<Integer> getSupportedJavaVersions() {
        return supportedJavaVersions;
    }

    public void setSupportedJavaVersions(List<Integer> supportedJavaVersions) {
        this.supportedJavaVersions = supportedJavaVersions;
    }
}
