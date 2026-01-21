package com.sequenceiq.cloudbreak.vm;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vm")
public class VirtualMachineConfiguration {

    private String supportedJavaVersions;

    private Map<String, String> supportedJavaVersionsByRuntime;

    private Set<Integer> processedSupportedJavaVersions;

    private Map<String, Set<Integer>> processedSupportedJavaVersionsByRuntime;

    @PostConstruct
    public void init() {
        processedSupportedJavaVersions = Arrays.stream(supportedJavaVersions.split(","))
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
        processedSupportedJavaVersionsByRuntime = supportedJavaVersionsByRuntime.entrySet().stream()
                .map(entry -> {
                    Set<Integer> value = Arrays.stream(entry.getValue().split(","))
                            .map(String::trim)
                            .filter(Predicate.not(String::isEmpty))
                            .map(Integer::parseInt)
                            .collect(Collectors.toSet());
                    return Map.entry(entry.getKey(), value);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void setSupportedJavaVersions(String supportedJavaVersions) {
        this.supportedJavaVersions = supportedJavaVersions;
    }

    public void setSupportedJavaVersionsByRuntime(Map<String, String> supportedJavaVersionsByRuntime) {
        this.supportedJavaVersionsByRuntime = supportedJavaVersionsByRuntime;
    }

    public Set<Integer> getSupportedJavaVersions() {
        return processedSupportedJavaVersions;
    }

    public Map<String, Set<Integer>> getSupportedJavaVersionsByRuntime() {
        return processedSupportedJavaVersionsByRuntime;
    }

    public boolean isJavaVersionSupported(String runtime, int version) {
        return processedSupportedJavaVersionsByRuntime.containsKey(runtime) && processedSupportedJavaVersionsByRuntime.get(runtime).contains(version);
    }

    public boolean isJavaVersionSupported(int version) {
        return processedSupportedJavaVersions.contains(version);
    }
}
