package com.sequenceiq.cloudbreak.service.java;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.java.vm.DefaultJavaConfigurations;
import com.sequenceiq.cloudbreak.service.java.vm.JavaConfiguration;

@Service
public class JavaDefaultVersionCalculator {

    @Inject
    private DefaultJavaConfigurations defaultJavaConfigurations;

    public int calculate(Integer javaVersion, String runtimeVersion) {
        if (javaVersion == null) {
            Optional<JavaConfiguration> javaConfiguration = defaultJavaConfigurations.defaultJavaConfigurationsAsList()
                    .stream()
                    .filter(d -> d.isRuntimeCompatible(runtimeVersion))
                    .findFirst();
            if (javaConfiguration.isPresent()) {
                return javaConfiguration.get().getVersion();
            } else {
                throw new BadRequestException(String.format("The runtimeVersion %s is not supported in CDP. " +
                        "Please change the runtime version.", runtimeVersion));
            }
        }
        return javaVersion;
    }
}
