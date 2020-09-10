package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import java.util.Optional;

import javax.inject.Inject;

import com.sequenceiq.freeipa.service.freeipa.user.model.Conversions;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Component
public class UmsCredentialProvider {

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public WorkloadCredential getCredentials(String userCrn, Optional<String> requestId) {
        GetActorWorkloadCredentialsResponse response =
                grpcUmsClient.getActorWorkloadCredentials(INTERNAL_ACTOR_CRN, userCrn, requestId);

        return Conversions.toWorkloadCredential(response);
    }
}
