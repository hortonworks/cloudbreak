package com.sequenceiq.cloudbreak.auth;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Component
public class ClouderaManagerLicenseProvider {

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private CMLicenseParser cmLicenseParser;

    public JsonCMLicense getLicense(String userCrn) {
        String accountId = getAccountIdFromCrn(userCrn);
        UserManagementProto.Account account = umsClient.getAccountDetails(INTERNAL_ACTOR_CRN, accountId, MDCUtils.getRequestId());
        return cmLicenseParser.parseLicense(account.getClouderaManagerLicenseKey())
                .orElseThrow(() -> new BadRequestException("No valid CM license is present"));
    }

    private String getAccountIdFromCrn(String crnStr) {
        try {
            Crn crn = Crn.safeFromString(crnStr);
            return crn.getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            throw new BadRequestException("Can not parse CRN to find account ID: " + crnStr);
        }
    }

}
