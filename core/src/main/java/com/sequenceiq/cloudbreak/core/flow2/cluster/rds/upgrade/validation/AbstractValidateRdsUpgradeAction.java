package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.AbstractValidateRdsUpgradeEvent;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

public abstract class AbstractValidateRdsUpgradeAction<P extends AbstractValidateRdsUpgradeEvent>
        extends AbstractStackAction<FlowState, FlowEvent, ValidateRdsUpgradeContext, P> {

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseService databaseService;

    protected AbstractValidateRdsUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ValidateRdsUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        Database database = databaseService.findById(stack.getDatabaseId()).orElse(new Database());
        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(cluster);
        return new ValidateRdsUpgradeContext(flowParameters, stack, cluster, database);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ValidateRdsUpgradeContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}