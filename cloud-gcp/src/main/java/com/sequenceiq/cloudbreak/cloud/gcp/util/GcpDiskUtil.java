package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.MachineType;

@Service
public class GcpDiskUtil {
    public static final int HYPERDISK_BALANCED_MIN_SIZE_GB = 4;

    public static final int HYPERDISK_BALANCED_MAX_SIZE_GB = 65536;

    public static final int HYPERDISK_EXTREME_MIN_SIZE_GB = 64;

    public static final int HYPERDISK_EXTREME_MAX_SIZE_GB = 65536;

    public static final int HYPERDISK_THROUGHPUT_MIN_SIZE_GB = 2048;

    public static final int HYPERDISK_THROUGHPUT_MAX_SIZE_GB = 32768;

    private static final Set<String> MACHINE_TYPES_WITH_HYPER_DISK_BALANCED = Set.of(
            "a3", "a4", "a4x", "c3", "c3d", "c4", "c4a", "c4d", "g4", "h3", "h4d", "m1", "m2", "m3", "m4", "n4", "n4a", "n4d", "x4", "z3");

    private static final Map<String, Integer> MACHINE_TYPES_WITH_HYPER_DISK_EXTREME = Map.ofEntries(
            Map.entry("a3", 0),
            Map.entry("a4", 0),
            Map.entry("a4x", 0),
            Map.entry("c3", 88),
            Map.entry("c3d", 60),
            Map.entry("c4", 96),
            Map.entry("c4a", 64),
            Map.entry("c4d", 64),
            Map.entry("g4", 96),
            Map.entry("m1", 80),
            Map.entry("m2", 0),
            Map.entry("m3", 64),
            Map.entry("m4", 64),
            Map.entry("n2", 80),
            Map.entry("x4", 0),
            Map.entry("z3", 0)
    );

    private static final Set<String> MACHINE_TYPES_WITH_HYPER_DISK_THROUGHTPUT = Set.of(
            "a3", "c3", "c3d", "c4", "c4a", "g2", "g4", "h3", "m3", "n2", "n2d", "n4", "n4a", "n4d", "t2d", "z3");

    @Inject
    private GcpStackUtil gcpStackUtil;

    public boolean isHyperdiskBalancedSupportedForInstanceType(String machineType) {
        return StringUtils.isNotBlank(machineType) && MACHINE_TYPES_WITH_HYPER_DISK_BALANCED.contains(gcpStackUtil.getMachineTypeFamily(machineType));
    }

    public boolean isHyperdiskBalancedSupportedForInstanceType(MachineType machineType) {
        return machineType != null && isHyperdiskBalancedSupportedForInstanceType(machineType.getName());
    }

    public boolean isHyperdiskExtremeSupportedForInstanceType(MachineType machineType) {
        boolean result = false;
        if (machineType != null) {
            Integer minCpu = MACHINE_TYPES_WITH_HYPER_DISK_EXTREME.get(gcpStackUtil.getMachineTypeFamily(machineType));
            if (minCpu != null && machineType.getGuestCpus() >= minCpu) {
                result = true;
            }
        }
        return result;
    }

    public boolean isHyperdiskThroughtputSupportedForInstanceType(MachineType machineType) {
        return machineType != null && MACHINE_TYPES_WITH_HYPER_DISK_THROUGHTPUT.contains(gcpStackUtil.getMachineTypeFamily(machineType));
    }
}
