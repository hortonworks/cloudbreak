package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep.CLOUDBREAK_ROTATE_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.context.CloudbreakPollerRotationContext;

@Component
public class CloudbreakPollerRotationExecutor implements RotationExecutor<CloudbreakPollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    public void rotate(CloudbreakPollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rotate cloudbreak secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType().name(), ROTATE);
            LOGGER.info("Rotate cloudbreak secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rotate cloudbreak secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rotate cloudbreak secret failed", e, getType());
        }
    }

    @Override
    public void rollback(CloudbreakPollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rollback cloudbreak secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType().name(), ROLLBACK);
            LOGGER.info("Rollback cloudbreak secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rollback cloudbreak secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rollback cloudbreak secret failed", e, getType());
        }
    }

    @Override
    public void finalize(CloudbreakPollerRotationContext rotationContext) {
        try {
            LOGGER.info("Finalize cloudbreak secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType().name(), FINALIZE);
            LOGGER.info("Finalize cloudbreak secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Finalize cloudbreak secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Finalize cloudbreak secret failed", e, getType());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return CLOUDBREAK_ROTATE_POLLING;
    }

    @Override
    public Class<CloudbreakPollerRotationContext> getContextClass() {
        return CloudbreakPollerRotationContext.class;
    }
}
