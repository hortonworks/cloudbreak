package com.sequenceiq.thunderhead.grpc.service.auth;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder.INTERNAL_ACCOUNT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.thunderhead.grpc.GrpcActorContext;

import io.grpc.Status;

@Service
class MockCrnService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCrnService.class);

    Crn createCrn(String accountId, CrnResourceDescriptor resourceDescriptor, String resource) {
        return Crn.builder()
                .setRegion(Crn.Region.US_WEST_1)
                .setPartition(Crn.Partition.CDP)
                .setService(resourceDescriptor.getServiceType())
                .setResourceType(resourceDescriptor.getResourceType())
                .setResource(resource)
                .setAccountId(accountId)
                .build();
    }

    void ensureInternalActor() {
        // For some reason the mock ums translates it to UNKNOWN
        String actorCrn = GrpcActorContext.ACTOR_CONTEXT.get().getActorCrn();
        LOGGER.info("Ensure internal actor: {}", actorCrn);
        if (!INTERNAL_ACTOR_CRN.equals(actorCrn)) {
            throw Status.PERMISSION_DENIED.withDescription("This operation is only allowed for internal services").asRuntimeException();
        }
    }

    void ensureProperAccountIdUsage(String accountId) {
        LOGGER.info("Ensure correct account id: {}", accountId);
        if (INTERNAL_ACCOUNT.equals(accountId)) {
            throw Status.INVALID_ARGUMENT.withDescription("This operation cannot be used with internal account id").asRuntimeException();
        }
    }
}
