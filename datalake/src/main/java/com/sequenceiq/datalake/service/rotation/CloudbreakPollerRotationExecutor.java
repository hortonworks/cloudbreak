package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CLOUDBREAK_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;

@Component
public class CloudbreakPollerRotationExecutor extends AbstractRotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakPollerRotationExecutor.class);

    @Inject
    private SdxRotationService sdxRotationService;

    @Override
    protected void rotate(PollerRotationContext rotationContext) {
        LOGGER.info("Rotate cloudbreak secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), ROTATE);
    }

    @Override
    protected void rollback(PollerRotationContext rotationContext) {
        LOGGER.info("Rollback cloudbreak secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), ROLLBACK);
    }

    @Override
    protected void finalize(PollerRotationContext rotationContext) {
        LOGGER.info("Finalize cloudbreak secret: {}", rotationContext.getSecretType());
        sdxRotationService.rotateCloudbreakSecret(rotationContext.getResourceCrn(), rotationContext.getSecretType(), FINALIZE);
    }

    @Override
    protected void preValidate(PollerRotationContext rotationContext) {
        sdxRotationService.preValidateCloudbreakRotation(rotationContext.getResourceCrn());
    }

    @Override
    protected void postValidate(PollerRotationContext rotationContext) {
    }

    @Override
    public SecretRotationStep getType() {
        return CLOUDBREAK_ROTATE_POLLING;
    }

    @Override
    protected Class<PollerRotationContext> getContextClass() {
        return PollerRotationContext.class;
    }
}
