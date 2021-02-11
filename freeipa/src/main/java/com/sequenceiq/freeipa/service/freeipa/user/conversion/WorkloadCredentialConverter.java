package com.sequenceiq.freeipa.service.freeipa.user.conversion;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.freeipa.service.freeipa.user.model.WorkloadCredential;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class WorkloadCredentialConverter {
    public WorkloadCredential toWorkloadCredential(
            UserManagementProto.ActorWorkloadCredentials actorWorkloadCredentials) {
        return new WorkloadCredential(actorWorkloadCredentials.getPasswordHash(),
                actorWorkloadCredentials.getKerberosKeysList(),
                toOptionalInstant(actorWorkloadCredentials.getPasswordHashExpirationDate()),
                actorWorkloadCredentials.getSshPublicKeyList(),
                actorWorkloadCredentials.getWorkloadCredentialsVersion());
    }

    public WorkloadCredential toWorkloadCredential(
            UserManagementProto.GetActorWorkloadCredentialsResponse actorWorkloadCredentials) {
        return new WorkloadCredential(actorWorkloadCredentials.getPasswordHash(),
                actorWorkloadCredentials.getKerberosKeysList(),
                toOptionalInstant(actorWorkloadCredentials.getPasswordHashExpirationDate()),
                actorWorkloadCredentials.getSshPublicKeyList(),
                actorWorkloadCredentials.getWorkloadCredentialsVersion());
    }

    private Optional<Instant> toOptionalInstant(long epochMillis) {
        return epochMillis == 0 ?
                Optional.empty() : Optional.of(Instant.ofEpochMilli(epochMillis));
    }
}
