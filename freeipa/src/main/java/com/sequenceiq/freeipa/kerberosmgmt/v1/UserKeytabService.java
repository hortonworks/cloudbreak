package com.sequenceiq.freeipa.kerberosmgmt.v1;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.KerberosConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

import java.util.List;

import static com.sequenceiq.freeipa.controller.exception.NotFoundException.notFound;

@Service
public class UserKeytabService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserKeytabService.class);

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private UserKeytabGenerator userKeytabGenerator;

    private String getKerberosRealm(String accountId, String environmentCrn) {
        KerberosConfig krbConfig =  kerberosConfigRepository
                .findByAccountIdAndEnvironmentCrnAndClusterNameIsNull(accountId, environmentCrn)
                .orElseThrow(notFound("KerberosConfig for environment", environmentCrn));
        return krbConfig.getRealm();
    }

    private void validateSameAccount(String userAccountId, String environmentCrn) {
        String environmentCrnAccountId = Crn.safeFromString(environmentCrn).getAccountId();
        if (!environmentCrnAccountId.equals(userAccountId)) {
            throw new BadRequestException("User and Environment must be in the same account");
        }
    }

    public String getKeytabBase64(String userCrn, String environmentCrn) {
        String userAccountId = Crn.safeFromString(userCrn).getAccountId();
        validateSameAccount(userAccountId, environmentCrn);

        String realm = getKerberosRealm(userAccountId, environmentCrn);

        GetActorWorkloadCredentialsResponse getActorWorkloadCredentialsResponse =
                grpcUmsClient.getActorWorkloadCredentials(IAM_INTERNAL_ACTOR_CRN, userCrn, MDCUtils.getRequestId());
        String workloadUsername = getActorWorkloadCredentialsResponse.getWorkloadUsername();
        List<ActorKerberosKey> actorKerberosKeys = getActorWorkloadCredentialsResponse.getKerberosKeysList();
        return userKeytabGenerator.generateKeytabBase64(workloadUsername, realm, actorKerberosKeys);
    }
}
