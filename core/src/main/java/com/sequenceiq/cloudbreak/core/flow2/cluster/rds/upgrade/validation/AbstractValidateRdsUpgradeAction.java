package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
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

    public static final String TARGET_MAJOR_VERSION_KEY = "TARGET_MAJOR_VERSION";

    public static final String CANARY_RDS_PROPERTIES_KEY = "CANARY_RDS_PROPERTIES";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseService databaseService;

    protected AbstractValidateRdsUpgradeAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ValidateRdsUpgradeContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> clusterContext, P payload) {
        Map<Object, Object> variables = clusterContext.getExtendedState().getVariables();

        StackView stack = stackDtoService.getStackViewById(payload.getResourceId());
        ClusterView cluster = stackDtoService.getClusterViewByStackId(payload.getResourceId());
        Database database = databaseService.findById(stack.getDatabaseId()).orElse(new Database());

        TargetMajorVersion targetMajorVersion = (TargetMajorVersion) variables.get(TARGET_MAJOR_VERSION_KEY);
        Object canaryPropertiesObject = variables.get(CANARY_RDS_PROPERTIES_KEY);
        DatabaseConnectionProperties canaryProperties = Objects.nonNull(canaryPropertiesObject) ? (DatabaseConnectionProperties) canaryPropertiesObject : null;

        MDCBuilder.buildMdcContext(stack);
        MDCBuilder.buildMdcContext(cluster);
        return new ValidateRdsUpgradeContext(flowParameters, stack, cluster, database, targetMajorVersion, canaryProperties);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ValidateRdsUpgradeContext> flowContext, Exception ex) {
        return new StackFailureEvent(payload.getResourceId(), ex);
    }
}