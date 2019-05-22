package com.sequenceiq.redbeams.service.crn;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Service
public class CrnService {

    // FIXME Account id is to be fixed - maybe comes from user CRN via CrnFilter / RestRequestThreadLocalService
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
                .setAccountId("ACCOUNT_ID")
                .setResourceType(resourceType)
                .setResource(resourceId)
                .build();
    }
}
