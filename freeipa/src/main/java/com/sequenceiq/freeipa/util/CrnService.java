package com.sequenceiq.freeipa.util;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CrnService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnService.class);

    @Inject
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    public String getCurrentAccountId() {
        String userCrn = threadBaseUserCrnProvider.getUserCrn();
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
        String userCrn = threadBaseUserCrnProvider.getUserCrn();
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
        return threadBaseUserCrnProvider.getUserCrn();
    }

    public String createCrn(String accountId, Crn.ResourceType resourceType) {
        return Crn.builder()
                .setService(Crn.Service.FREEIPA)
                .setAccountId(accountId)
                .setResourceType(resourceType)
                .setResource(UUID.randomUUID().toString())
                .build().toString();
    }

    public static String checkUserCrn(String userCrn) {
        if (userCrn == null) {
            throw new CrnParseException("User CRN must be provided");
        }
        return userCrn;
    }

}
