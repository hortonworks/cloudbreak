package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class MigrateZookeeperToKraftContext extends CommonContext {

    private final boolean kraftInstallNeeded;

    private final boolean staleConfigsOnly;

    private final boolean kraftHostGroupPresent;

    private Long stackId;

    public MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        kraftInstallNeeded = false;
        staleConfigsOnly = false;
        kraftHostGroupPresent = false;
        stackId = event.getResourceId();
    }

    public MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event, boolean kraftInstallNeeded) {
        super(flowParameters);
        stackId = event.getResourceId();
        this.kraftInstallNeeded = kraftInstallNeeded;
        this.staleConfigsOnly = false;
        this.kraftHostGroupPresent = false;
    }

    private MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event, boolean staleConfigsOnly,
            boolean kraftHostGroupPresent) {
        super(flowParameters);
        stackId = event.getResourceId();
        this.kraftInstallNeeded = false;
        this.staleConfigsOnly = staleConfigsOnly;
        this.kraftHostGroupPresent = kraftHostGroupPresent;
    }

    public static MigrateZookeeperToKraftContext from(FlowParameters flowParameters, StackEvent event) {
        return new MigrateZookeeperToKraftContext(flowParameters, event);
    }

    public static MigrateZookeeperToKraftContext from(FlowParameters flowParameters, StackEvent event, boolean kraftInstallNeeded) {
        return new MigrateZookeeperToKraftContext(flowParameters, event, kraftInstallNeeded);
    }

    public static MigrateZookeeperToKraftContext fromMigration(FlowParameters flowParameters, StackEvent event, boolean staleConfigsOnly,
            boolean kraftHostGroupPresent) {
        return new MigrateZookeeperToKraftContext(flowParameters, event, staleConfigsOnly, kraftHostGroupPresent);
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public boolean isKraftInstallNeeded() {
        return kraftInstallNeeded;
    }

    public boolean isStaleConfigsOnly() {
        return staleConfigsOnly;
    }

    public boolean isKraftHostGroupPresent() {
        return kraftHostGroupPresent;
    }

}
