package com.sequenceiq.cloudbreak.service.java.vm;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
@ConfigurationProperties(prefix = "vm.allowable")
public class AllowableJavaConfigurations {

    private List<JavaConfiguration> javaVersions;

    public List<JavaConfiguration> getJavaVersions() {
        return javaVersions;
    }

    public void setJavaVersions(List<JavaConfiguration> javaVersions) {
        this.javaVersions = javaVersions;
    }

    public void checkValidConfiguration(int version, String runtimeVersion) {
        boolean valid = javaVersions.stream()
                .anyMatch(javaConfiguration -> javaConfiguration.getVersion() == version && javaConfiguration.isRuntimeCompatible(runtimeVersion));
        if (!valid) {
            throw new BadRequestException("The requested Java version " + version + " is not compatible with the runtime version " + runtimeVersion);
        }
    }

    public List<String> listValidJavaVersions(String runtimeVersion) {
        return javaVersions.stream()
                .filter(javaConfiguration -> runtimeVersion == null || javaConfiguration.isRuntimeCompatible(runtimeVersion))
                .map(javaConfiguration -> String.valueOf(javaConfiguration.getVersion()))
                .toList();
    }
}
