package com.sequenceiq.freeipa.service.rotation.pillar.executor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep;
import com.sequenceiq.freeipa.service.freeipa.user.AdminUserService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;
import com.sequenceiq.freeipa.service.rotation.pillar.context.FreeIpaPillarRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaPillarRotationExecutor extends AbstractRotationExecutor<FreeIpaPillarRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPillarRotationExecutor.class);

    @Inject
    private AdminUserService adminUserService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Override
    public void rotate(FreeIpaPillarRotationContext rotationContext) throws Exception {
        LOGGER.info("Update pillar for rotate phase");
        updatePillar(rotationContext);
    }

    @Override
    public void rollback(FreeIpaPillarRotationContext rotationContext) {
        LOGGER.info("Update pillar for rollback phase");
        updatePillar(rotationContext);
    }

    private void updatePillar(FreeIpaPillarRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        LOGGER.info("Update pillar for: {}", stack.getResourceCrn());
        adminUserService.updateFreeIpaPillar(stack);
    }

    @Override
    public void finalize(FreeIpaPillarRotationContext rotationContext) {

    }

    @Override
    public void preValidate(FreeIpaPillarRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        LOGGER.info("Validate for pillar update: {}", stack.getResourceCrn());
        try {
            freeIpaSaltPingService.saltPing(stack);
        } catch (SaltPingFailedException e) {
            throw new CloudbreakRuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void postValidate(FreeIpaPillarRotationContext rotationContext) {

    }

    @Override
    public SecretRotationStep getType() {
        return FreeIpaSecretRotationStep.FREEIPA_PILLAR_UPDATE;
    }

    @Override
    public Class<FreeIpaPillarRotationContext> getContextClass() {
        return FreeIpaPillarRotationContext.class;
    }
}
