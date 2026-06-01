package com.sequenceiq.cloudbreak.auth.crn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class AccountIdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountIdService.class);

    public String getAccountIdFromResourceCrn(String resourceCrn) {
        try {
            return Crn.safeFromString(resourceCrn).getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            LOGGER.warn("Can not parse CRN to find account ID: {}", resourceCrn, e);
            throw new BadRequestException("Can not parse CRN to find account ID: " + resourceCrn);
        }
    }
}
