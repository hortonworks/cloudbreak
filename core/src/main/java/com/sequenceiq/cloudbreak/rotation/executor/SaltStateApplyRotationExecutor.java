package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.SaltStateApplyRotationContext;

@Component
public class SaltStateApplyRotationExecutor extends AbstractRotationExecutor<SaltStateApplyRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStateApplyRotationExecutor.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    protected void rotate(SaltStateApplyRotationContext context) throws Exception {
        LOGGER.info("Executing salt states [{}] regarding secret rotation.", Joiner.on(",").join(context.getStates()));
        hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), context.getStates(), context.getExitCriteriaModel(),
                context.getMaxRetry(), context.getMaxRetryOnError());
    }

    @Override
    protected void rollback(SaltStateApplyRotationContext context) throws Exception {
        List<String> rollbackStates = context.getStates();
        if (context.getRollBackStates().isPresent()) {
            rollbackStates = context.getRollBackStates().get();
        }
        LOGGER.info("Executing salt states [{}] regarding rollback of secret rotation.", Joiner.on(",").join(rollbackStates));
        hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), rollbackStates, context.getExitCriteriaModel(),
                context.getMaxRetry(), context.getMaxRetryOnError());
    }

    @Override
    protected void finalize(SaltStateApplyRotationContext context) throws Exception {
        executeStatesIfPresent(context.getCleanupStates(), "finalization", context);
    }

    @Override
    protected void preValidate(SaltStateApplyRotationContext context) throws Exception {
        hostOrchestrator.ping(context.getTargets(), context.getGatewayConfig());
        executeStatesIfPresent(context.getPreValidateStates(), "pre validation", context);
    }

    @Override
    protected void postValidate(SaltStateApplyRotationContext context) throws Exception {
        executeStatesIfPresent(context.getPostValidateStates(), "post validation", context);
    }

    private void executeStatesIfPresent(Optional<List<String>> states, String message, SaltStateApplyRotationContext context)
            throws CloudbreakOrchestratorFailedException {
        if (states.isPresent()) {
            LOGGER.info("Executing salt states [{}] regarding {} of secret rotation.",
                    Joiner.on(",").join(states.get()), message);
            hostOrchestrator.executeSaltState(context.getGatewayConfig(), context.getTargets(), states.get(),
                    context.getExitCriteriaModel(), context.getMaxRetry(), context.getMaxRetryOnError());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return SALT_STATE_APPLY;
    }

    @Override
    protected Class<SaltStateApplyRotationContext> getContextClass() {
        return SaltStateApplyRotationContext.class;
    }
}
