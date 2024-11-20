package com.sequenceiq.cloudbreak.rotation.executor;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class SaltPillarRotationExecutor extends AbstractRotationExecutor<SaltPillarRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPillarRotationExecutor.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private SecretRotationSaltService saltService;

    @Override
    protected void rotate(SaltPillarRotationContext rotationContext) throws Exception {
        updateSaltPillar(rotationContext);
    }

    @Override
    protected void rollback(SaltPillarRotationContext rotationContext) throws Exception {
        updateSaltPillar(rotationContext);
    }

    private void updateSaltPillar(SaltPillarRotationContext rotationContext) throws CloudbreakOrchestratorFailedException {
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        Map<String, SaltPillarProperties> servicePillar = rotationContext.getServicePillarGenerator().apply(stackDto);
        saltService.updateSaltPillar(stackDto, servicePillar);
    }

    @Override
    protected void finalize(SaltPillarRotationContext rotationContext) {
        LOGGER.info("Finalize salt pillar rotation, nothing to do.");
    }

    @Override
    protected void preValidate(SaltPillarRotationContext rotationContext) throws Exception {
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        saltService.validateSalt(stackDto);
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
