package com.sequenceiq.cloudbreak.rotation.executor;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.context.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;

@Component
public class CustomJobExecutor implements RotationExecutor<CustomJobRotationContext> {

    @Override
    public void rotate(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getRotationJob().stream().forEach(Runnable::run);
    }

    @Override
    public void rollback(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getRollbackJob().stream().forEach(Runnable::run);
    }

    @Override
    public void finalize(CustomJobRotationContext rotationContext) throws Exception {
        rotationContext.getFinalizeJob().stream().forEach(Runnable::run);
    }

    @Override
    public void preValidate(CustomJobRotationContext rotationContext) throws Exception {

    }

    @Override
    public void postValidate(CustomJobRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return CommonSecretRotationStep.CUSTOM_JOB;
    }

    @Override
    public Class<CustomJobRotationContext> getContextClass() {
        return CustomJobRotationContext.class;
    }
}
