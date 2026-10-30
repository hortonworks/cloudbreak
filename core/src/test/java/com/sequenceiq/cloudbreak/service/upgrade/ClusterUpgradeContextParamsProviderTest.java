package com.sequenceiq.cloudbreak.service.upgrade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClusterUpgradeContextParamsProviderTest {

    @Test
    void nestedFlowChainsInheritOnlyTheUpgradeProperties() {
        assertThat(new ClusterUpgradeContextParamsProvider().inheritedContextParamKeys()).containsExactly(ClusterUpgradeProperties.FLOW_VARIABLE_NAME);
    }
}
