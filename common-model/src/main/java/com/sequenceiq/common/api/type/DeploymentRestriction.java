package com.sequenceiq.common.api.type;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum DeploymentRestriction {
    FREEIPA,
    DATALAKE,
    DATAHUB,
    MLX,
    DEX,
    DFX,
    DWX,
    LIFTIE,
    ENDPOINT_ACCESS_GATEWAY;

    public static final Set<DeploymentRestriction> ALL = Set.of(values());

    public static final Set<DeploymentRestriction> NON_ENDPOINT_ACCESS_GATEWAYS =
            Arrays.stream(values()).filter(v -> v != ENDPOINT_ACCESS_GATEWAY).collect(Collectors.toUnmodifiableSet());

    public static final Set<DeploymentRestriction> ENDPOINT_ACCESS_GATEWAYS = Set.of(ENDPOINT_ACCESS_GATEWAY);

}
