package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowParameters;

public class ValidateRdsUpgradeContext extends ClusterViewContext {

    private final Database database;

    private final TargetMajorVersion targetMajorVersion;

    private final DatabaseConnectionProperties canaryProperties;

    public ValidateRdsUpgradeContext(FlowParameters flowParameters, StackView stack, ClusterView cluster, Database database,
            TargetMajorVersion targetMajorVersion, DatabaseConnectionProperties canaryProperties) {
        super(flowParameters, stack, cluster);
        this.database = database;
        this.targetMajorVersion = targetMajorVersion;
        this.canaryProperties = canaryProperties;
    }

    public Database getDatabase() {
        return database;
    }

    public DatabaseConnectionProperties getCanaryProperties() {
        return canaryProperties;
    }

    public TargetMajorVersion getTargetMajorVersion() {
        return targetMajorVersion;
    }
}