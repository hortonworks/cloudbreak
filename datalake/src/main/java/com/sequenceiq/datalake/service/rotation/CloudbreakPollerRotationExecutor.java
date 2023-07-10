package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

@Component
public class CloudbreakPollerRotationExecutor extends AbstractRotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    public void rotate(PollerRotationContext rotationContext) {
        LOGGER.info("Rotate cloudbreak secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), (CloudbreakSecretType) rotationContext.getSecretType(), ROTATE);
    }

    @Override
    public void rollback(PollerRotationContext rotationContext) {
        LOGGER.info("Rollback cloudbreak secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), (CloudbreakSecretType) rotationContext.getSecretType(), ROLLBACK);
    }

    @Override
    public void finalize(PollerRotationContext rotationContext) {
        LOGGER.info("Finalize cloudbreak secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), (CloudbreakSecretType) rotationContext.getSecretType(), FINALIZE);
    }

    @Override
    public void preValidate(PollerRotationContext rotationContext) {
        sdxRotationService.preValidateCloudbreakRotation(rotationContext.getResourceCrn());
    }

    @Override
    public void postValidate(PollerRotationContext rotationContext) {
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
