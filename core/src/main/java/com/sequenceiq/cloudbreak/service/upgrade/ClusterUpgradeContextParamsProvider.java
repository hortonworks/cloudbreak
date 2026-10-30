package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties.FLOW_VARIABLE_NAME;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.chain.InheritedFlowChainContextParamsProvider;

@Component
public class ClusterUpgradeContextParamsProvider implements InheritedFlowChainContextParamsProvider {

    @Override
    public Set<String> inheritedContextParamKeys() {
        return Set.of(FLOW_VARIABLE_NAME);
    }
}
