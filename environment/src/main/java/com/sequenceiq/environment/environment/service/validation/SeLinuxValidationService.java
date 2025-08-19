package com.sequenceiq.environment.environment.service.validation;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@Service
public class SeLinuxValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxValidationService.class);

    @Inject
    private EntitlementService entitlementService;

    public void validateSeLinuxEntitlementGrantedForFreeipaCreation(FreeIpaCreationDto freeIpaCreationDto) {
        SeLinux seLinuxMode = Optional.ofNullable(freeIpaCreationDto.getSeLinux()).orElse(SeLinux.PERMISSIVE);
        if (SeLinux.ENFORCING.equals(seLinuxMode)) {
            boolean entitled = entitlementService.isCdpSecurityEnforcingSELinux(ThreadBasedUserCrnProvider.getAccountId());
            LOGGER.debug("SELinux entitlement granted for account '{}': '{}'", ThreadBasedUserCrnProvider.getAccountId(), entitled);
            if (!entitled) {
                throw new CloudbreakServiceException("You are not entitled to use SELinux enforcing mode. " +
                        "Please contact your CDP administrator about the enablement of this feature!");
            }
        }
    }
}
