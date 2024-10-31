package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.MachineType;

// https://cloud.google.com/compute/docs/disks/extreme-persistent-disk#machine_shape_support
@Service
public class ExtremeDiskCalculator {

    private static final String N2 = "n2";

    private static final String M2 = "m2";

    private static final String M3 = "m3";

    private static final int MINIMUM_CPU_ON_N2_INSTANCES = 64;

    private static final Set<String> MACHINE_TYPES_WITH_EXTREME_SSD = Set.of(M2, N2, M3);

    public boolean extremeDiskSupported(MachineType machineType) {
        boolean supported = false;
        String machineTypeFamily = getMachineTypeFamily(machineType);
        if (MACHINE_TYPES_WITH_EXTREME_SSD.contains(machineTypeFamily)) {
            switch (machineTypeFamily) {
                case N2:
                    if (machineType.getGuestCpus() >= MINIMUM_CPU_ON_N2_INSTANCES) {
                        supported = true;
                    }
                    break;
                case M2, M3:
                    supported = true;
                    break;
                default:
                    break;
            }
        }
        return supported;
    }

    private String getMachineTypeFamily(MachineType machineType) {
        return machineType.getName().toLowerCase(Locale.ROOT).split("-")[0];
    }

}
