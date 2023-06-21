package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.context.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

@Component
public class RedbeamsPollerRotationExecutor implements RotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    public void rotate(PollerRotationContext rotationContext) {
        LOGGER.info("Rotate redbeams secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateRedbeamsSecret(rotationContext.getResourceCrn(), (RedbeamsSecretType) rotationContext.getSecretType(), ROTATE);
    }

    @Override
    public void rollback(PollerRotationContext rotationContext) {
        LOGGER.info("Rollback redbeams secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateRedbeamsSecret(rotationContext.getResourceCrn(), (RedbeamsSecretType) rotationContext.getSecretType(), ROLLBACK);
    }

    @Override
    public void finalize(PollerRotationContext rotationContext) {
        LOGGER.info("Finalize redbeams secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateRedbeamsSecret(rotationContext.getResourceCrn(), (RedbeamsSecretType) rotationContext.getSecretType(), FINALIZE);
    }

    @Override
    public void preValidate(PollerRotationContext rotationContext) throws Exception {

    }

    @Override
    public void postValidate(PollerRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return REDBEAMS_ROTATE_POLLING;
    }

    @Override
    public Class<PollerRotationContext> getContextClass() {
        return PollerRotationContext.class;
    }
}
