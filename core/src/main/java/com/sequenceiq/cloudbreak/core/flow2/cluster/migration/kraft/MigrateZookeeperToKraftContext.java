package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class MigrateZookeeperToKraftContext extends CommonContext {

    private final boolean kraftInstallNeeded;

    private final boolean staleConfigsOnly;

    private Long stackId;

    public MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        kraftInstallNeeded = false;
        staleConfigsOnly = false;
        stackId = event.getResourceId();
    }

    public MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event, boolean kraftInstallNeeded) {
        super(flowParameters);
        stackId = event.getResourceId();
        this.kraftInstallNeeded = kraftInstallNeeded;
        this.staleConfigsOnly = false;
    }

    public MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event, boolean kraftInstallNeeded,
            boolean staleConfigsOnly) {
        super(flowParameters);
        stackId = event.getResourceId();
        this.kraftInstallNeeded = kraftInstallNeeded;
        this.staleConfigsOnly = staleConfigsOnly;
    }

    public static MigrateZookeeperToKraftContext from(FlowParameters flowParameters, StackEvent event) {
        return new MigrateZookeeperToKraftContext(flowParameters, event);
    }

    public static MigrateZookeeperToKraftContext from(FlowParameters flowParameters, StackEvent event, boolean kraftInstallNeeded) {
        return new MigrateZookeeperToKraftContext(flowParameters, event, kraftInstallNeeded);
    }

    public static MigrateZookeeperToKraftContext from(FlowParameters flowParameters, StackEvent event, boolean kraftInstallNeeded,
            boolean staleConfigsOnly) {
        return new MigrateZookeeperToKraftContext(flowParameters, event, kraftInstallNeeded, staleConfigsOnly);
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

}
