package com.sequenceiq.authorization.utils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

@Component
public class CrnAccountValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnAccountValidator.class);

    public void validateSameAccount(String userCrn, String resourceCrn) {
        validateSameAccount(userCrn, List.of(resourceCrn));
    }

    public void validateSameAccount(String userCrnValue, Collection<String> resourceCrns) {
        if ((Crn.isCrn(userCrnValue) && RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrnValue))
                || CollectionUtils.isEmpty(resourceCrns)) {
            return;
        }
        Crn userCrn = Crn.ofUser(userCrnValue);
        for (String resourceCrn : resourceCrns) {
            validateSameAccount(userCrn, Crn.safeFromString(resourceCrn));
        }
    }

    private void validateSameAccount(Crn userCrn, Crn resourceCrn) {
        if (!Objects.equals(userCrn.getAccountId(), resourceCrn.getAccountId())) {
            LOGGER.warn("User {} tried to access {} from different account.", userCrn, resourceCrn);
            throw new AccessDeniedException(String.format("Can't access resource from different account.", resourceCrn, userCrn.getAccountId()));
        }
    }
}
