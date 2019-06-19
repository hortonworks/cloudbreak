package com.sequenceiq.redbeams.service.crn;

import java.util.UUID;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Service
public class CrnService {

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public String getCurrentAccountId() {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new CrnParseException("Current user CRN is not set");
        }
        Crn crn = Crn.safeFromString(userCrn);
        return crn.getAccountId();
    }

    public String getCurrentUserId() {
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new CrnParseException("Current user CRN is not set");
        }
        Crn crn = Crn.safeFromString(userCrn);
        return crn.getResource();
    }

    public Crn createCrn(DatabaseConfig resource) {
        return createCrn(resource, Crn.ResourceType.DATABASE);
    }

    public Crn createCrn(DatabaseServerConfig resource) {
        return createCrn(resource, Crn.ResourceType.DATABASE_SERVER);
    }

    private Crn createCrn(Object resource, Crn.ResourceType resourceType) {
        if (resource == null) {
            throw new IllegalArgumentException("Cannot create CRN for null resource");
        }

        String resourceId = UUID.randomUUID().toString();

        return Crn.builder()
                .setService(Crn.Service.REDBEAMS)
                .setAccountId(getCurrentAccountId())
                .setResourceType(resourceType)
                .setResource(resourceId)
                .build();
    }
}
