package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class UpgradeEmbeddedDBPreparationContext extends ClusterViewContext {

    private final TargetMajorVersion version;

    public UpgradeEmbeddedDBPreparationContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, TargetMajorVersion version) {
        super(flowParameters, stack, cluster);
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }
}
