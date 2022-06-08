package com.sequenceiq.freeipa.service.freeipa.user.ums;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.WorkloadCredentialConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;

@Component
public class UmsCredentialProvider {

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private WorkloadCredentialConverter workloadCredentialConverter;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public WorkloadCredential getCredentials(String userCrn) {
        GetActorWorkloadCredentialsResponse response =
                grpcUmsClient.getActorWorkloadCredentials(userCrn, regionAwareInternalCrnGeneratorFactory);

        return workloadCredentialConverter.toWorkloadCredential(response);
    }
}
