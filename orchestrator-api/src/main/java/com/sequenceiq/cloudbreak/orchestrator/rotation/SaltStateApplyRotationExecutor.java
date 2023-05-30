package com.sequenceiq.cloudbreak.orchestrator.rotation;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;

@Component
public class SaltStateApplyRotationExecutor implements RotationExecutor<SaltStateApplyRotationContext> {

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public void rotate(SaltStateApplyRotationContext context) {
        try {
            hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), context.getStates(), context.getExitCriteriaModel());
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException(e, getType());
        }
    }

    @Override
    public void rollback(SaltStateApplyRotationContext context) {
        try {
            List<String> rollbackStates = context.getStates();
            if (context.getRollBackStates().isPresent()) {
                rollbackStates = context.getRollBackStates().get();
            }
            hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), rollbackStates, context.getExitCriteriaModel());
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException(e, getType());
        }
    }

    @Override
    public void finalize(SaltStateApplyRotationContext context) {
        if (context.getCleanupStates().isPresent()) {
            try {
                hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(),
                        context.getCleanupStates().get(), context.getExitCriteriaModel());
            } catch (CloudbreakOrchestratorFailedException e) {
                throw new SecretRotationException(e, getType());
            }
        }
    }

    @Override
    public SecretRotationStep getType() {
        return SecretRotationStep.SALT_STATE_APPLY;
    }

    @Override
    public Class<SaltStateApplyRotationContext> getContextClass() {
        return SaltStateApplyRotationContext.class;
    }
}
