package com.sequenceiq.common.api.type;

import java.util.List;

public enum Tunnel {
    DIRECT, CCM, CLUSTER_PROXY;

    private static final List<Tunnel> USE_CP_LIST = List.of(CCM, CLUSTER_PROXY);

    public boolean useCcm() {
        return this == CCM;
    }

    public boolean useClusterProxy() {
        return USE_CP_LIST.contains(this);
    }
}
