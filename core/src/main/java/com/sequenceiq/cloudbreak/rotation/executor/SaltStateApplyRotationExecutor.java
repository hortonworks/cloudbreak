package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@Component
public class SaltStateApplyRotationExecutor implements RotationExecutor<SaltStateApplyRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStateApplyRotationExecutor.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public void rotate(SaltStateApplyRotationContext context) throws Exception {
        LOGGER.info("Executing salt states [{}] regarding secret rotation.", Joiner.on(",").join(context.getStates()));
        hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), context.getStates(), context.getExitCriteriaModel(),
                context.getMaxRetry(), context.getMaxRetryOnError());
    }

    @Override
    public void rollback(SaltStateApplyRotationContext context) throws Exception {
        List<String> rollbackStates = context.getStates();
        if (context.getRollBackStates().isPresent()) {
            rollbackStates = context.getRollBackStates().get();
        }
        LOGGER.info("Executing salt states [{}] regarding rollback of secret rotation.", Joiner.on(",").join(rollbackStates));
        hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), rollbackStates, context.getExitCriteriaModel(),
                context.getMaxRetry(), context.getMaxRetryOnError());
    }

    @Override
    public void finalize(SaltStateApplyRotationContext context) throws Exception {
        if (context.getCleanupStates().isPresent()) {
            LOGGER.info("Executing salt states [{}] regarding finalization of secret rotation.",
                    Joiner.on(",").join(context.getCleanupStates().get()));
            hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), context.getCleanupStates().get(),
                    context.getExitCriteriaModel(), context.getMaxRetry(), context.getMaxRetryOnError());
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
