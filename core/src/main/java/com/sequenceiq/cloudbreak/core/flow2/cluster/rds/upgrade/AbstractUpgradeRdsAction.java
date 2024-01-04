package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.AbstractUpgradeRdsEvent;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractUpgradeRdsAction<P extends AbstractUpgradeRdsEvent> extends AbstractStackAction<FlowState, FlowEvent, UpgradeRdsContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseService databaseService;

    protected AbstractUpgradeRdsAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected UpgradeRdsContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        Database database = databaseService.findById(stack.getDatabaseId()).orElse(new Database());
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(cluster);
        return new UpgradeRdsContext(flowParameters, stack, cluster, database, payload.getVersion());
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<UpgradeRdsContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}
