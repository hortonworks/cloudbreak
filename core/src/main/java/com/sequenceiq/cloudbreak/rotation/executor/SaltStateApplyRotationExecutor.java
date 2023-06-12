package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@Component
public class SaltStateApplyRotationExecutor implements RotationExecutor<SaltStateApplyRotationContext> {

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public void rotate(SaltStateApplyRotationContext context) throws Exception {
        hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), context.getStates(), context.getExitCriteriaModel());
    }

    @Override
    public void rollback(SaltStateApplyRotationContext context) throws Exception {
        List<String> rollbackStates = context.getStates();
        if (context.getRollBackStates().isPresent()) {
            rollbackStates = context.getRollBackStates().get();
        }
        hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), rollbackStates, context.getExitCriteriaModel());
    }

    @Override
    public void finalize(SaltStateApplyRotationContext context) throws Exception {
        if (context.getCleanupStates().isPresent()) {
            hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(),
                    context.getCleanupStates().get(), context.getExitCriteriaModel());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return SALT_STATE_APPLY;
    }

    @Override
    public Class<SaltStateApplyRotationContext> getContextClass() {
        return SaltStateApplyRotationContext.class;
    }
}
