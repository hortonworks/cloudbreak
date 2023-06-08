package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.context.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@Component
public class CloudbreakPollerRotationExecutor implements RotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    public void rotate(PollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rotate cloudbreak secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), (CloudbreakSecretType) rotationContext.getSecretType(), ROTATE);
            LOGGER.info("Rotate cloudbreak secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rotate cloudbreak secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rotate cloudbreak secret failed", e, getType());
        }
    }

    @Override
    public void rollback(PollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rollback cloudbreak secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), (CloudbreakSecretType) rotationContext.getSecretType(), ROLLBACK);
            LOGGER.info("Rollback cloudbreak secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rollback cloudbreak secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rollback cloudbreak secret failed", e, getType());
        }
    }

    @Override
    public void finalize(PollerRotationContext rotationContext) {
        try {
            LOGGER.info("Finalize cloudbreak secret started for {}", rotationContext.getResourceCrn());
            sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), (CloudbreakSecretType) rotationContext.getSecretType(), FINALIZE);
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
    public Class<PollerRotationContext> getContextClass() {
        return PollerRotationContext.class;
    }
}
