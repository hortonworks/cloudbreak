package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class SaltPillarRotationExecutor implements RotationExecutor<SaltPillarRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPillarRotationExecutor.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 100;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Override
    public void rotate(SaltPillarRotationContext rotationContext) {
        updateSaltPillar(rotationContext, "rotation");
    }

    private void updateSaltPillar(SaltPillarRotationContext rotationContext, String rotationState) {
        LOGGER.debug("Salt pillar {} for {}", rotationState, rotationContext.getResourceCrn());
        try {
            StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
            Map<String, SaltPillarProperties> servicePillar = rotationContext.getServicePillarGenerator().apply(rotationContext.getResourceCrn());
            hostOrchestrator.saveCustomPillars(new SaltConfig(servicePillar),
                    new ClusterDeletionBasedExitCriteriaModel(stackDto.getId(), stackDto.getCluster().getId()),
                    saltStateParamsService.createStateParams(stackDto, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR));
        } catch (Exception e) {
            String errorMessage = String.format("Salt pillar %s failed for %s", rotationState, rotationContext.getResourceCrn());
            LOGGER.error(errorMessage, e);
            throw new SecretRotationException(errorMessage, e, getType());
        }
        LOGGER.debug("Salt pillar {} finished for {}", rotationState, rotationContext.getResourceCrn());
    }

    @Override
    public void rollback(SaltPillarRotationContext rotationContext) {
        updateSaltPillar(rotationContext, "rollback");
    }

    @Override
    public void finalize(SaltPillarRotationContext rotationContext) {
        LOGGER.debug("Finalize salt pillar for {}", rotationContext.getResourceCrn());
    }

    @Override
    public SecretRotationStep getType() {
        return CloudbreakSecretRotationStep.SALT_PILLAR;
    }

    @Override
    public Class<SaltPillarRotationContext> getContextClass() {
        return SaltPillarRotationContext.class;
    }
}
