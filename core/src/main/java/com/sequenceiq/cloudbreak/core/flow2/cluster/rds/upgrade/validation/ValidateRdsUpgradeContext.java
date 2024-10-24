package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ValidateRdsUpgradeContext extends ClusterViewContext {

    private final Database database;

    public ValidateRdsUpgradeContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, Database database) {
        super(flowParameters, stack, cluster);
        this.database = database;
    }

    public Database getDatabase() {
        return database;
    }
}