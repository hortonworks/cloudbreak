package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.step.SecretRotationStep;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaAdminPasswordRotationUtil {

    @Inject
    private StackService stackService;

    public void checkRedhat8(FreeIpaAdminPasswordRotationContext rotationContext, SecretRotationStep secretRotationStep) {
        String resourceCrn = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(resourceCrn);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(resourceCrn, environmentCrn.getAccountId());
        if (!"redhat8".equals(stack.getImage().getOsType())) {
            throw new SecretRotationException("Freeipa admin password rotation is supported only on Redhat 8", secretRotationStep);
        }
    }

}
