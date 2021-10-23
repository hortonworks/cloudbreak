package com.sequenceiq.common.api.type;

import java.util.List;

public enum Tunnel {
    DIRECT, CCM, CLUSTER_PROXY, CCMV2, CCMV2_JUMPGATE;

    private static final List<Tunnel> USE_CP_LIST = List.of(CCM, CCMV2, CLUSTER_PROXY, CCMV2_JUMPGATE);

    public static Tunnel latestUpgradeTarget() {
        return CCMV2_JUMPGATE;
    }

    public boolean useCcm() {
        return this == CCM || this == CCMV2 || this == CCMV2_JUMPGATE;
    }

    public boolean useCcmV1() {
        return this == CCM;
    }

    public boolean useCcmV2() {
        return this == CCMV2;
    }

    public boolean useCcmV2OrJumpgate() {
        return this == CCMV2 || this == CCMV2_JUMPGATE;
    }

    public boolean useCcmV2Jumpgate() {
        return this == CCMV2_JUMPGATE;
    }

    public boolean useClusterProxy() {
        return USE_CP_LIST.contains(this);
    }
}
