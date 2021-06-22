package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Service
class ClouderaManagerLicenseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLicenseService.class);

    @Inject
    private GrpcUmsClient umsClient;

    void validateClouderaManagerLicense(User user) {
        if (hasNoLicense(user.getUserCrn())) {
            LOGGER.error("User '{}' doesn't have a valid cloudera manager license.", user.getUserCrn());
            throw new RuntimeException("User doesn't have a valid cloudera manager license.");
        }
    }

    private boolean hasNoLicense(String userCrn) {
        return StringUtils.isEmpty(umsClient.getAccountDetails(Crn.safeFromString(userCrn).getAccountId(), Optional.empty())
                .getClouderaManagerLicenseKey());
    }

}
