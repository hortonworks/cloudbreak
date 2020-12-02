package com.sequenceiq.common.api.type;

import java.util.List;

public enum Tunnel {
    DIRECT, CCM, CLUSTER_PROXY, CCMV2;

    private static final List<Tunnel> USE_CP_LIST = List.of(CCM, CCMV2, CLUSTER_PROXY);

    public boolean useCcm() {
        return this == CCM || this == CCMV2;
    }

    public boolean useCcmV1() {
        return this == CCM;
    }

    public boolean useCcmV2() {
        return this == CCMV2;
    }

    public boolean useClusterProxy() {
        return USE_CP_LIST.contains(this);
    }
}
