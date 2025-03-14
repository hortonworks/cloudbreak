package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.SkuMigrationFinished;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.attach.AttachPublicIpsAddLBResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.check.CheckSkuResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.detachpublicips.DetachPublicIpsResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.handler.removeloadbalancer.RemoveLoadBalancerResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SkuMigrationFlowEvent implements FlowEvent {

    SKU_MIGRATION_EVENT,
    SKU_MIGRATION_CHECK_LOAD_BALANCER_EVENT,
    SKU_MIGRATION_CHECK_SKU_FINISHED_EVENT(EventSelectorUtil.selector(CheckSkuResult.class)),
    SKU_MIGRATION_DETACH_PUBLIC_IPS_FINISHED_EVENT(EventSelectorUtil.selector(DetachPublicIpsResult.class)),
    SKU_MIGRATION_REMOVE_LOAD_BALANCER_FINISHED_EVENT(EventSelectorUtil.selector(RemoveLoadBalancerResult.class)),
    SKU_MIGRATION_ATTACH_PUBLIC_IPS_ADD_LB_FINISHED_EVENT(EventSelectorUtil.selector(AttachPublicIpsAddLBResult.class)),
    SKU_MIGRATION_FINISHED_EVENT(EventSelectorUtil.selector(SkuMigrationFinished.class)),
    SKU_MIGRATION_FINALIZED_EVENT,
    SKU_MIGRATION_FAIL_HANDLED_EVENT,
    SKU_MIGRATION_FAILED_EVENT(EventSelectorUtil.selector(SkuMigrationFailedEvent.class));

    private final String event;

    SkuMigrationFlowEvent() {
        event = name();
    }

    SkuMigrationFlowEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
