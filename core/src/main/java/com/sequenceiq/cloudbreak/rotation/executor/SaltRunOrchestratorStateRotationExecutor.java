package com.sequenceiq.cloudbreak.rotation.executor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.SaltRunOrchestratorStateRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@Component
public class SaltRunOrchestratorStateRotationExecutor implements RotationExecutor<SaltRunOrchestratorStateRotationContext> {

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public void rotate(SaltRunOrchestratorStateRotationContext rotationContext) throws Exception {
        if (rotationContext.stateRunNeeded()) {
            hostOrchestrator.runOrchestratorState(rotationContext.getRotateOrchestratorStateParams());
        }
    }

    @Override
    public void rollback(SaltRunOrchestratorStateRotationContext rotationContext) throws Exception {
        if (rotationContext.stateRunNeeded() && rotationContext.rollbackStateExists()) {
            hostOrchestrator.runOrchestratorState(rotationContext.getRollbackOrchestratorStateParams());
        }
    }

    @Override
    public void finalize(SaltRunOrchestratorStateRotationContext rotationContext) throws Exception {
        if (rotationContext.stateRunNeeded() && rotationContext.cleanupStateExists()) {
            hostOrchestrator.runOrchestratorState(rotationContext.getCleanupOrchestratorStateParams());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.SALT_STATE_RUN;
    }

    @Override
    public Class<SaltRunOrchestratorStateRotationContext> getContextClass() {
        return SaltRunOrchestratorStateRotationContext.class;
    }
}
