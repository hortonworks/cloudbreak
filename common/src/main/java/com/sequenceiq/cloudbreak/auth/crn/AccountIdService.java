package com.sequenceiq.cloudbreak.auth.crn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class AccountIdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountIdService.class);

    public String getAccountIdFromUserCrn(String userCrn) {
        try {
            return Crn.safeFromString(userCrn).getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            LOGGER.warn("Can not parse CRN to find account ID: {}", userCrn, e);
            throw new BadRequestException("Can not parse CRN to find account ID: " + userCrn);
        }
    }
}
