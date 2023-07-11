package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext;

@Component
public class SaltRunOrchestratorStateRotationExecutor extends AbstractRotationExecutor<SaltRunOrchestratorStateRotationContext> {

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    protected void rotate(SaltRunOrchestratorStateRotationContext context) throws Exception {
        if (context.stateRunNeeded()) {
            hostOrchestrator.runOrchestratorState(getStateParams(Optional.of(context.getRotateParams()), Optional.of(context.getStates()), context));
        }
    }

    @Override
    protected void rollback(SaltRunOrchestratorStateRotationContext context) throws Exception {
        if (context.stateRunNeeded() && context.rollbackStateExists()) {
            hostOrchestrator.runOrchestratorState(getStateParams(context.getRollbackParams(), context.getRollBackStates(), context));
        }
    }

    @Override
    protected void finalize(SaltRunOrchestratorStateRotationContext context) throws Exception {
        if (context.stateRunNeeded() && context.cleanupStateExists()) {
            hostOrchestrator.runOrchestratorState(getStateParams(context.getCleanupParams(), context.getCleanupStates(), context));
        }
    }

    @Override
    protected void preValidate(SaltRunOrchestratorStateRotationContext context) throws Exception {
        hostOrchestrator.ping(context.getTargets(), context.getGatewayConfig());
        if (context.stateRunNeeded() && context.preValidateStateExists()) {
            hostOrchestrator.runOrchestratorState(getStateParams(context.getPrevalidateParams(), context.getPreValidateStates(), context));
        }
    }

    @Override
    protected void postValidate(SaltRunOrchestratorStateRotationContext context) throws Exception {
        if (context.stateRunNeeded() && context.postValidateStateExists()) {
            hostOrchestrator.runOrchestratorState(getStateParams(context.getPostValidateParams(), context.getPostValidateStates(), context));
        }
    }

    private OrchestratorStateParams getStateParams(Optional<Map<String, Object>> params, Optional<List<String>> states,
            SaltRunOrchestratorStateRotationContext context) {
        OrchestratorStateParams stateParams = context.getBaseParams();
        stateParams.setState(context.getStateFromOptionalStates(states));
        stateParams.setStateParams(params.orElse(null));
        return stateParams;
    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.SALT_STATE_RUN;
    }

    @Override
    protected Class<SaltRunOrchestratorStateRotationContext> getContextClass() {
        return SaltRunOrchestratorStateRotationContext.class;
    }
}
