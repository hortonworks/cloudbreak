package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.REDBEAMS_ROTATE_POLLING;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class RedbeamsPollerRotationExecutor extends AbstractRotationExecutor<PollerRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsPollerRotationExecutor.class);

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Inject
    private StackDtoService stackDtoService;

    @Override
    protected void rotate(PollerRotationContext rotationContext) {
        LOGGER.info("Rotate redbeams secret: {}", rotationContext.getSecretType());
        StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
        externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(), rotationContext.getSecretType(), ROTATE,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void rollback(PollerRotationContext rotationContext) {
        LOGGER.info("Rollback redbeams secret: {}", rotationContext.getSecretType());
        StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
        externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(), rotationContext.getSecretType(), ROLLBACK,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void finalizeRotation(PollerRotationContext rotationContext) {
        LOGGER.info("Finalize redbeams secret: {}", rotationContext.getSecretType());
        StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
        externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(), rotationContext.getSecretType(), FINALIZE,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void preValidate(PollerRotationContext rotationContext) {
        LOGGER.info("Pre validate redbeams secret rotation: {}", rotationContext.getSecretType());
        StackDto stackDto = getStackDto(rotationContext.getResourceCrn());
        externalDatabaseService.preValidateDatabaseSecretRotation(stackDto.getCluster().getDatabaseServerCrn());
        externalDatabaseService.rotateDatabaseSecret(stackDto.getCluster().getDatabaseServerCrn(), rotationContext.getSecretType(), PREVALIDATE,
                rotationContext.getAdditionalProperties());
    }

    @Override
    protected void postValidate(PollerRotationContext rotationContext) {
    }

    @Override
    public SecretRotationStep getType() {
        return REDBEAMS_ROTATE_POLLING;
    }

    @Override
    protected Class<PollerRotationContext> getContextClass() {
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
