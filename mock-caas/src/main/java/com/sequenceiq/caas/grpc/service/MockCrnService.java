package com.sequenceiq.caas.grpc.service;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;

import org.springframework.stereotype.Service;

import com.sequenceiq.caas.grpc.GrpcActorContext;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

import io.grpc.Status;

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

    void ensureInternalActor() {
        // For some reason the mock ums translates it to UNKNOWN
        String actorCrn = GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn();
        if (!INTERNAL_ACTOR_CRN.equals(actorCrn)) {
            throw Status.PERMISSION_DENIED.withDescription("This operation is only allowed for internal services").asRuntimeException();
        }
    }
}
