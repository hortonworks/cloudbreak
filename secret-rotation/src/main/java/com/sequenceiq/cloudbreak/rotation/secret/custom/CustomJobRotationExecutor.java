package com.sequenceiq.cloudbreak.rotation.secret.custom;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;

@Component
public class CustomJobRotationExecutor extends AbstractRotationExecutor<CustomJobRotationContext> {

    @Override
    protected void rotate(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getRotationJob().ifPresent(Runnable::run);
    }

    @Override
    protected void rollback(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getRollbackJob().ifPresent(Runnable::run);
    }

    @Override
    protected void finalize(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getFinalizeJob().ifPresent(Runnable::run);
    }

    @Override
    protected void preValidate(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getPreValidateJob().ifPresent(Runnable::run);
    }

    @Override
    protected void postValidate(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getPostValidateJob().ifPresent(Runnable::run);
    }

    @Override
    public SecretRotationStep getType() {
        return CommonSecretRotationStep.CUSTOM_JOB;
    }

    @Override
    protected Class<CustomJobRotationContext> getContextClass() {
        return CustomJobRotationContext.class;
    }
}
