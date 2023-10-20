package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class UpgradeRdsContext extends ClusterViewContext {

    private final Database database;

    private final TargetMajorVersion version;

    public UpgradeRdsContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, Database database, TargetMajorVersion version) {
        super(flowParameters, stack, cluster);
        this.database = database;
        this.version = version;
    }

    public Database getDatabase() {
        return database;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }
}
