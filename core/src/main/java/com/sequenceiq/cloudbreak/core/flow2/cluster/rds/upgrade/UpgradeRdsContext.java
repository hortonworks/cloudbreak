package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class UpgradeRdsContext extends ClusterViewContext {

    private final TargetMajorVersion version;

    public UpgradeRdsContext(FlowParameters flowParameters, StackView stack, TargetMajorVersion version) {
        super(flowParameters, stack);
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }
}
