package com.sequenceiq.cloudbreak.cm;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
class ClouderaManagerLicenseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLicenseService.class);

    @Inject
    private GrpcUmsClient umsClient;

    void validateClouderaManagerLicense(String accountId) {
        if (hasNoLicense(accountId)) {
            LOGGER.error("For tenant '{}' there is no valid cloudera manager license.", accountId);
            throw new RuntimeException("For this tenant there is no valid cloudera manager license.");
        }
    }

    private boolean hasNoLicense(String accountId) {
        return StringUtils.isEmpty(umsClient.getAccountDetails(accountId, MDCUtils.getRequestId())
                .getClouderaManagerLicenseKey());
    }

}
