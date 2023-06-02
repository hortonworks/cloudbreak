package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.REDBEAMS_ROTATE_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.context.RedbeamsPollerRotationContext;

@Component
public class RedbeamsPollerRotationExecutor implements RotationExecutor<RedbeamsPollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    public void rotate(RedbeamsPollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rotate redbeams secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateRedbeamsSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType().name(), ROTATE);
            LOGGER.info("Rotate redbeams secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rotate redbeams secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rotate redbeams secret failed", e, getType());
        }
    }

    @Override
    public void rollback(RedbeamsPollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rollback redbeams secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateRedbeamsSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType().name(), ROLLBACK);
            LOGGER.info("Rollback redbeams secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rollback redbeams secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rollback redbeams secret failed", e, getType());
        }
    }

    @Override
    public void finalize(RedbeamsPollerRotationContext rotationContext) {
        try {
            LOGGER.info("Finalize redbeams secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateRedbeamsSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType().name(), FINALIZE);
            LOGGER.info("Finalize redbeams secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Finalize redbeams secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Finalize redbeams secret failed", e, getType());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return REDBEAMS_ROTATE_POLLING;
    }

    @Override
    public Class<RedbeamsPollerRotationContext> getContextClass() {
        return RedbeamsPollerRotationContext.class;
    }
}
