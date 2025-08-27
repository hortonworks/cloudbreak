package com.sequenceiq.freeipa.service.rotation.adminpassword.executor;

import static com.sequenceiq.common.model.OsType.RHEL8;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.adminpassword.context.FreeIpaAdminPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaAdminPasswordRotationUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaAdminPasswordRotationUtil.class);

    @Inject
    private StackService stackService;

    public void checkRedhat8(FreeIpaAdminPasswordRotationContext rotationContext) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        String osType = stack.getImage().getOsType();
        if (!RHEL8.getOs().equalsIgnoreCase(osType)) {
            LOGGER.info("OS type is not REDHAT 8: {}", osType);
            throw new SecretRotationException("Freeipa admin password rotation is supported only on Redhat 8");
        }
    }

}
