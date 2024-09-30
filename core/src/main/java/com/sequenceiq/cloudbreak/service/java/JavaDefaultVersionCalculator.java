package com.sequenceiq.cloudbreak.service.java;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.java.vm.DefaultJavaConfigurations;

@Service
public class JavaDefaultVersionCalculator {

    @Inject
    private DefaultJavaConfigurations defaultJavaConfigurations;

    public int calculate(Integer javaVersion, String runtimeVersion) {
        if (javaVersion == null) {
            return defaultJavaConfigurations.defaultJavaConfigurationsAsList()
                    .stream()
                    .filter(d -> d.isRuntimeCompatible(runtimeVersion))
                    .findFirst()
                    .get()
                    .getVersion();
        }
        return javaVersion;
    }
}
