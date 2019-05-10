package com.sequenceiq.redbeams.service.crn;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
public class CrnService {

    // FIXME Account id is to be fixed
    public Crn createDatabaseCrnFrom(String resource) {
        return Crn.builder()
                .setService(Crn.Service.REDBEAMS)
                .setAccountId("ACCOUNT_ID")
                .setResourceType(Crn.ResourceType.DATABASE)
                .setResource(resource)
                .build();
    }

    public Crn createDatabaseCrn() {
        return createDatabaseCrnFrom(UUID.randomUUID().toString());
    }
}
