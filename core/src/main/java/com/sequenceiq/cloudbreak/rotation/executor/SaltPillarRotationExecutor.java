package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class SaltPillarRotationExecutor extends AbstractRotationExecutor<SaltPillarRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPillarRotationExecutor.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 100;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Override
    protected void rotate(SaltPillarRotationContext rotationContext) throws Exception {
        updateSaltPillar(rotationContext, "rotation");
    }

    @Override
    protected void rollback(SaltPillarRotationContext rotationContext) throws Exception {
        updateSaltPillar(rotationContext, "rollback");
    }

    private void updateSaltPillar(SaltPillarRotationContext rotationContext, String rotationState) throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        Map<String, SaltPillarProperties> servicePillar = rotationContext.getServicePillarGenerator().apply(stackDto);
        LOGGER.info("Salt pillar {}, keys: {}", rotationState, servicePillar.keySet());
        hostOrchestrator.saveCustomPillars(new SaltConfig(servicePillar), exitCriteriaProvider.get(stackDto),
                saltStateParamsService.createStateParams(stackDto, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR));
    }

    @Override
    protected void finalize(SaltPillarRotationContext rotationContext) {
        LOGGER.info("Finalize salt pillar rotation, nothing to do.");
    }

    @Override
    protected void preValidate(SaltPillarRotationContext rotationContext) throws Exception {
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        OrchestratorStateParams stateParams =
                saltStateParamsService.createStateParams(stackDto, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
        hostOrchestrator.ping(stateParams.getTargetHostNames(), stateParams.getPrimaryGatewayConfig());
    }

    @Override
    protected void postValidate(SaltPillarRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.SALT_PILLAR;
    }

    @Override
    protected Class<SaltPillarRotationContext> getContextClass() {
        return SaltPillarRotationContext.class;
    }
}
