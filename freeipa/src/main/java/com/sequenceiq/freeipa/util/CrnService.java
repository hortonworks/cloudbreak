package com.sequenceiq.freeipa.util;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;

@Component
public class CrnService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnService.class);

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public String getCurrentAccountId() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Crn crn = null;
        try {
            crn = Crn.fromString(userCrn);
        } catch (NullPointerException e) {
            LOGGER.warn("Crn is not set", e);
            throw new CrnParseException("CRN is not set");
        }
        if (crn != null) {
            return crn.getAccountId();
        } else {
            throw new CrnParseException("Can not parse account ID from CRN");
        }
    }

    public String getCurrentUserId() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Crn crn = null;
        try {
            crn = Crn.fromString(userCrn);
        } catch (NullPointerException e) {
            LOGGER.warn("Crn is not set", e);
            throw new CrnParseException("CRN is not set");
        }
        if (crn != null) {
            if (ResourceType.USER.equals(crn.getResourceType())) {
                return crn.getResource();
            } else {
                return null;
            }
        } else {
            throw new CrnParseException("Can not parse account ID from CRN");
        }
    }

    public String getUserCrn() {
        return ThreadBasedUserCrnProvider.getUserCrn();
    }

    public String createCrn(String accountId, CrnResourceDescriptor resourceDescriptor) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(resourceDescriptor, accountId);
    }
}
