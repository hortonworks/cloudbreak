package com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class MigrateZookeeperToKraftContext extends CommonContext {

    private Long stackId;

    public MigrateZookeeperToKraftContext(FlowParameters flowParameters, StackEvent event) {
        super(flowParameters);
        stackId = event.getResourceId();
    }

    public static MigrateZookeeperToKraftContext from(FlowParameters flowParameters, StackEvent event) {
        return new MigrateZookeeperToKraftContext(flowParameters, event);
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }
}
