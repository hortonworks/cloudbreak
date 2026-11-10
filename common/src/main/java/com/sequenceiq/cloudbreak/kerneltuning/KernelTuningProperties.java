package com.sequenceiq.cloudbreak.kerneltuning;

import static java.util.Collections.singletonMap;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kernel memory tuning policy applied by the salt-common 'kernel-tuning' state on every provisioned RHEL 8+ node (Data Hub, Data Lake, FreeIPA).
 * The node computes vm.min_free_kbytes from its own memory on every highstate, so only the policy is shipped in the pillar.
 */
@Component
@ConfigurationProperties("cluster.kernel-tuning.min-free-kbytes")
public class KernelTuningProperties {

    public static final String PILLAR_KEY = "kernel-tuning";

    public static final String PILLAR_PATH = "/kernel-tuning/init.sls";

    private boolean enabled;

    private double ratioPercent;

    private long floorKb;

    private long restoreKb;

    public Map<String, Object> toPillarMap() {
        return singletonMap(PILLAR_KEY, singletonMap("minFreeKbytes", Map.of(
                "enabled", enabled,
                "ratioPercent", ratioPercent,
                "floorKb", floorKb,
                "restoreKb", restoreKb)));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getRatioPercent() {
        return ratioPercent;
    }

    public void setRatioPercent(double ratioPercent) {
        this.ratioPercent = ratioPercent;
    }

    public long getFloorKb() {
        return floorKb;
    }

    public void setFloorKb(long floorKb) {
        this.floorKb = floorKb;
    }

    public long getRestoreKb() {
        return restoreKb;
    }

    public void setRestoreKb(long restoreKb) {
        this.restoreKb = restoreKb;
    }

    @Override
    public String toString() {
        return "KernelTuningProperties{" +
                "enabled=" + enabled +
                ", ratioPercent=" + ratioPercent +
                ", floorKb=" + floorKb +
                ", restoreKb=" + restoreKb +
                '}';
    }
}
