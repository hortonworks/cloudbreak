package com.sequenceiq.caas.grpc.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
class MockCrnService {

    Crn createCrn(String baseCrn, Crn.ResourceType resourceType, String resource) {
        Crn crn = Crn.fromString(baseCrn);
        return createCrn(crn.getAccountId(), crn.getService(), resourceType, resource);
    }

    Crn createCrn(String accountId, Crn.Service service, Crn.ResourceType resourceType, String resource) {
        return Crn.builder()
                .setAccountId(accountId)
                .setService(service)
                .setResourceType(resourceType)
                .setResource(resource)
                .build();
    }
}
