package com.sequenceiq.datalake.service.rotation;


import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.FREEIPA_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

@Component
public class FreeipaPollerRotationExecutor extends AbstractRotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    protected void rotate(PollerRotationContext rotationContext) {
        LOGGER.info("Rotate redbeams secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateFreeipaSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), ROTATE,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void rollback(PollerRotationContext rotationContext) {
        LOGGER.info("Rollback redbeams secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateFreeipaSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), ROLLBACK,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void finalizeRotation(PollerRotationContext rotationContext) {
        LOGGER.info("Finalize redbeams secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateFreeipaSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), FINALIZE,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void preValidate(PollerRotationContext rotationContext) {
        sdxRotationService.preValidateFreeipaRotation(rotationContext.getResourceCrn());
        sdxRotationService.rotateFreeipaSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), PREVALIDATE,
                rotationContext.getAdditionalProperties());
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
