package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.api.services.compute.model.MachineType;

@ConfigurationProperties(prefix = "gcp.hyperdisk")
public class GcpInstanceTypeHyperDiskConfig {
    private final Map<String, InstanceFamilyConfig> familyTypes;

    private final Map<String, InstanceTypeConfig> instanceTypes;

    private final Map<Pattern, InstanceTypeConfig> instanceTypeRegexMap = new HashMap<>();

    public GcpInstanceTypeHyperDiskConfig(Map<String, InstanceFamilyConfig> familyTypes, Map<String, InstanceTypeConfig> instanceTypes) {
        this.familyTypes = familyTypes;
        this.instanceTypes = instanceTypes;
    }

    @PostConstruct
    public void init() {
        for (Map.Entry<String, InstanceTypeConfig> instanceTypeConfig : instanceTypes.entrySet()) {
            instanceTypeRegexMap.put(Pattern.compile(instanceTypeConfig.getKey()), instanceTypeConfig.getValue());
        }
    }

    public boolean isHyperdiskBalancedSupportedForInstanceType(String machineType) {
        return getFamilyConfig(machineType)
                .map(InstanceFamilyConfig::hyperDiskBalancedSupported)
                .orElse(false);
    }

    public Pair<Boolean, Boolean> isHyperdiskBalancedSupportedForAllInstanceType(List<String> machineTypes) {
        return machineTypes.stream()
                .map(this::getFamilyConfig)
                .map(configOptional -> Pair.of(configOptional.map(InstanceFamilyConfig::hyperDiskBalancedSupported).orElse(false),
                        configOptional.map(InstanceFamilyConfig::pdBalancedSupported).orElse(true)))
                .reduce(Pair.of(true, true), (result, next) ->
                        Pair.of(result.getLeft() && next.getLeft(), result.getRight() && next.getRight()));
    }

    public Optional<InstanceFamilyConfig> getFamilyConfig(MachineType machineType) {
        return machineType != null ? getFamilyConfig(machineType.getName()) : Optional.empty();
    }

    public Optional<InstanceFamilyConfig> getFamilyConfig(String machineType) {
        return Optional.ofNullable(machineType)
                .map(type -> familyTypes.get(getMachineTypeFamily(type)));
    }

    public Optional<InstanceTypeConfig> getInstanceTypeConfig(MachineType machineType) {
        return machineType != null ? getInstanceTypeConfig(machineType.getName()) : Optional.empty();
    }

    public Optional<InstanceTypeConfig> getInstanceTypeConfig(String machineType) {
        return machineType != null ? instanceTypeRegexMap.entrySet().stream()
                .filter(regexEntry -> regexEntry.getKey().matcher(machineType).matches())
                .map(Map.Entry::getValue)
                .findFirst() : Optional.empty();
    }

    public String getMachineTypeFamily(MachineType machineType) {
        return getMachineTypeFamily(machineType.getName());
    }

    private String getMachineTypeFamily(String machineType) {
        return machineType.toLowerCase(Locale.ROOT).split("-")[0];
    }

    public record InstanceFamilyConfig(
            String defaultDiskType,
            boolean pdStandardSupported,
            boolean pdSsdSupported,
            boolean pdBalancedSupported,
            boolean pdExtremeSupported,
            boolean hyperDiskBalancedSupported,
            boolean hyperDiskExtremeSupported,
            boolean hyperDiskThroughputSupported,
            int hyperDiskExtremeMinCpuCount) {
    }

    public record InstanceTypeConfig(
            int hyperDiskMaxCount,
            int hyperDiskBalancedMaxCount,
            int hyperDiskExtremeMaxCount,
            int hyperDiskThroughputMaxCount) {

        public int hyperDiskBalancedMaxCount() {
            return calculateCount(hyperDiskBalancedMaxCount, hyperDiskMaxCount);
        }

        public int hyperDiskExtremeMaxCount() {
            return calculateCount(hyperDiskExtremeMaxCount, hyperDiskMaxCount);
        }

        public int hyperDiskThroughputMaxCount() {
            return calculateCount(hyperDiskThroughputMaxCount, hyperDiskMaxCount);
        }

        private int calculateCount(int count, int maxCount) {
            return maxCount <= 0 ? count : Math.min(count, maxCount);
        }
    }
}
