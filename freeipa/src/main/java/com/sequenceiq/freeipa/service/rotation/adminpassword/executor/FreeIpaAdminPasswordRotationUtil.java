package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaAdminPasswordRotationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAdminPasswordRotationUtil.class);

    @Inject
    private StackService stackService;

    public void checkRedhat(RotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        String osType = stack.getImage().getOsType();
        if (!OsType.freeipaAdminPasswordRotationIsSupported(osType)) {
            LOGGER.info("OS type is not REDHAT: {}", osType);
            throw new SecretRotationException("Freeipa admin password rotation is supported only on Redhat");
        }
    }

}
