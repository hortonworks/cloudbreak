package com.sequenceiq.redbeams.service.crn;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.UuidGeneratorService;

@Service
public class CrnService {

    @Inject
    private UuidGeneratorService uuidGeneratorService;

    public String getCurrentAccountId() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new CrnParseException("Current user CRN is not set");
        }
        Crn crn = Crn.safeFromString(userCrn);
        return crn.getAccountId();
    }

    public String getCurrentUserId() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (userCrn == null) {
            throw new CrnParseException("Current user CRN is not set");
        }
        Crn crn = Crn.safeFromString(userCrn);
        return crn.getUserId();
    }

    public Crn createCrn(DatabaseConfig resource) {
        return createCrn(resource, Crn.ResourceType.DATABASE);
    }

    public Crn createCrn(DatabaseServerConfig resource) {
        return createCrn(resource, Crn.ResourceType.DATABASE_SERVER);
    }

    public Crn createCrn(DBStack resource) {
        // We want this resource to be DATABASE_SERVER as well, since this resource will end up
        // being attached to a database server.
        return createCrn(resource, Crn.ResourceType.DATABASE_SERVER);
    }

    private Crn createCrn(Object resource, Crn.ResourceType resourceType) {
        if (resource == null) {
            throw new IllegalArgumentException("Cannot create CRN for null resource");
        }

        String resourceId = uuidGeneratorService.randomUuid();

        return Crn.builder()
                .setService(Crn.Service.REDBEAMS)
                .setAccountId(getCurrentAccountId())
                .setResourceType(resourceType)
                .setResource(resourceId)
                .build();
    }
}
