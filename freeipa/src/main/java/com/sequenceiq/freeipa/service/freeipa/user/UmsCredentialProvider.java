package com.sequenceiq.freeipa.service.freeipa.user;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Optional;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;

@Component
public class UmsCredentialProvider {

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public WorkloadCredential getCredentials(String userCrn, Optional<String> requestId) {
        GetActorWorkloadCredentialsResponse response =
                grpcUmsClient.getActorWorkloadCredentials(INTERNAL_ACTOR_CRN, userCrn, requestId);
        long expirationDate = response.getPasswordHashExpirationDate();
        Optional<Instant> expirationInstant = expirationDate == 0 ?
                Optional.empty() : Optional.of(Instant.ofEpochMilli(expirationDate));
        return new WorkloadCredential(response.getPasswordHash(), response.getKerberosKeysList(), expirationInstant);
    }
}
