package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class FreeIpaPollerRotationExecutor extends AbstractRotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPollerRotationExecutor.class);

    @Inject
    private FreeipaService freeipaService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected void rotate(PollerRotationContext rotationContext) {
        LOGGER.info("Rotate FreeIpa secret: {}", rotationContext.getSecretType());
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        freeipaService.rotateFreeIpaSecret(stackDto.getEnvironmentCrn(), stackDto.getName(), rotationContext.getSecretType(), ROTATE);
    }

    @Override
    protected void rollback(PollerRotationContext rotationContext) {
        LOGGER.info("Rollback FreeIpa secret: {}", rotationContext.getSecretType());
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        freeipaService.rotateFreeIpaSecret(stackDto.getEnvironmentCrn(), stackDto.getName(), rotationContext.getSecretType(), ROLLBACK);
    }

    @Override
    protected void finalize(PollerRotationContext rotationContext) {
        LOGGER.info("Finalize FreeIpa secret: {}", rotationContext.getSecretType());
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        freeipaService.rotateFreeIpaSecret(stackDto.getEnvironmentCrn(), stackDto.getName(), rotationContext.getSecretType(), FINALIZE);
    }

    @Override
    protected void preValidate(PollerRotationContext rotationContext) {
        LOGGER.info("Pre validate FreeIpa secret rotation: {}", rotationContext.getSecretType());
        StackDto stackDto = stackDtoService.getByCrn(rotationContext.getResourceCrn());
        freeipaService.preValidateFreeIpaSecretRotation(stackDto.getEnvironmentCrn());
        freeipaService.rotateFreeIpaSecret(stackDto.getEnvironmentCrn(), stackDto.getName(), rotationContext.getSecretType(), PREVALIDATE);
    }

    @Override
    protected void postValidate(PollerRotationContext rotationContext) {
    }

    @Override
    public SecretRotationStep getType() {
        return FREEIPA_ROTATE_POLLING;
    }

    @Override
    protected Class<PollerRotationContext> getContextClass() {
        return PollerRotationContext.class;
    }

}
