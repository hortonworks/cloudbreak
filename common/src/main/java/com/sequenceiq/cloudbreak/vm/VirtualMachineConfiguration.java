package com.sequenceiq.cloudbreak.vm;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VirtualMachineConfiguration {

    private final Set<Integer> supportedJavaVersions;

    public VirtualMachineConfiguration(
            @Value("${vm.supportedJavaVersions}") String supportedJavaVersions) {
        this.supportedJavaVersions = Arrays.stream(supportedJavaVersions.split(","))
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    public Set<Integer> getSupportedJavaVersions() {
        return supportedJavaVersions;
    }
}
