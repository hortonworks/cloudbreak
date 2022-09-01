package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ValidateRdsUpgradeContext extends ClusterViewContext {

    public ValidateRdsUpgradeContext(FlowParameters flowParameters, StackView stack, ClusterView cluster) {
        super(flowParameters, stack, cluster);
    }
}
