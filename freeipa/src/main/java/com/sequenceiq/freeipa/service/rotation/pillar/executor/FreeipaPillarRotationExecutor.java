package com.sequenceiq.freeipa.service.rotation.pillar.executor;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.secret.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.user.AdminUserService;
import com.sequenceiq.freeipa.service.rotation.pillar.context.FreeipaPillarRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeipaPillarRotationExecutor extends AbstractRotationExecutor<FreeipaPillarRotationContext> {

    @Inject
    private AdminUserService adminUserService;

    @Inject
    private StackService stackService;

    @Override
    public void rotate(FreeipaPillarRotationContext rotationContext) throws Exception {
        updatePillar(rotationContext);
    }

    @Override
    public void rollback(FreeipaPillarRotationContext rotationContext) throws Exception {
        updatePillar(rotationContext);
    }

    private void updatePillar(FreeipaPillarRotationContext rotationContext) {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        adminUserService.updateFreeIpaPillar(stack);
    }

    @Override
    public void finalize(FreeipaPillarRotationContext rotationContext) {

    }

    @Override
    public void preValidate(FreeipaPillarRotationContext rotationContext) {

    }

    @Override
    public void postValidate(FreeipaPillarRotationContext rotationContext) {

    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.FREEIPA_PILLAR_UPDATE;
    }

    @Override
    public Class<FreeipaPillarRotationContext> getContextClass() {
        return FreeipaPillarRotationContext.class;
    }
}
