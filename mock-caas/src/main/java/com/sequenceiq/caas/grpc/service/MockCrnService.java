package com.sequenceiq.caas.grpc.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.caas.grpc.GrpcActorContext;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;

import io.grpc.Status;

@Service
class MockCrnService {

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

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
        if (!IAM_INTERNAL_ACTOR_CRN.equals(actorCrn)) {
            throw Status.PERMISSION_DENIED.withDescription("This operation is only allowed for internal services").asRuntimeException();
        }
    }
}
