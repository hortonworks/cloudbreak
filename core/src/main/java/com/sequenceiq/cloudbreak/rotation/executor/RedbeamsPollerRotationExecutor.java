package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.secret.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.context.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

@Component
public class RedbeamsPollerRotationExecutor implements RotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerRotationExecutor.class);

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public void rotate(PollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rotate redbeams secret started for {}", rotationContext.getResourceCrn());
            StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
            externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(),
                    (RedbeamsSecretType) rotationContext.getSecretType(), ROTATE);
            LOGGER.info("Rotate redbeams secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.warn("Rotate redbeams secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rotate redbeams secret failed", e, getType());
        }
    }

    @Override
    public void rollback(PollerRotationContext rotationContext) {
        try {
            LOGGER.info("Rollback redbeams secret started for {}", rotationContext.getResourceCrn());
            StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
            externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(),
                    (RedbeamsSecretType) rotationContext.getSecretType(), ROLLBACK);
            LOGGER.info("Rollback redbeams secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.info("Rollback redbeams secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Rollback redbeams secret failed", e, getType());
        }
    }

    @Override
    public void finalize(PollerRotationContext rotationContext) {
        try {
            LOGGER.info("Finalize redbeams secret started for {}", rotationContext.getResourceCrn());
            StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
            externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(),
                    (RedbeamsSecretType) rotationContext.getSecretType(), FINALIZE);
            LOGGER.info("Finalize redbeams secret finished for {}", rotationContext.getResourceCrn());
        } catch (Exception e) {
            LOGGER.info("Finalize redbeams secret failed for {}", rotationContext.getResourceCrn(), e);
            throw new SecretRotationException("Finalize redbeams secret failed", e, getType());
        }
    }

    @Override
    public SecretRotationStep getType() {
        return REDBEAMS_ROTATE_POLLING;
    }

    @Override
    public Class<PollerRotationContext> getContextClass() {
        return PollerRotationContext.class;
    }

    private StackDto getStackDto(String resourceCrn) {
        StackDto stackDto = stackDtoService.getByCrn(resourceCrn);
        if (stackDto.getCluster().getDatabaseServerCrn() == null) {
            throw new RuntimeException("No database server found for cluster: " + resourceCrn);
        }
        return stackDto;
    }
}
