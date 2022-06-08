package com.sequenceiq.cloudbreak.auth;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class ClouderaManagerLicenseProvider {

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private CMLicenseParser cmLicenseParser;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public JsonCMLicense getLicense(String userCrn) {
        String accountId = getAccountIdFromCrn(userCrn);
        UserManagementProto.Account account = umsClient.getAccountDetails(accountId, regionAwareInternalCrnGeneratorFactory);
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
