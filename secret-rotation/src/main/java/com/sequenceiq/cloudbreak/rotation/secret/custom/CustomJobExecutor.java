package com.sequenceiq.cloudbreak.rotation.secret.custom;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;

@Component
public class CustomJobExecutor extends AbstractRotationExecutor<CustomJobRotationContext> {

    @Override
    protected void rotate(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getRotationJob().stream().forEach(Runnable::run);
    }

    @Override
    protected void rollback(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getRollbackJob().stream().forEach(Runnable::run);
    }

    @Override
    protected void finalize(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getFinalizeJob().stream().forEach(Runnable::run);
    }

    @Override
    protected void preValidate(CustomJobRotationContext rotationContext) throws Exception {

    }

    @Override
    protected void postValidate(CustomJobRotationContext rotationContext) throws Exception {

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
