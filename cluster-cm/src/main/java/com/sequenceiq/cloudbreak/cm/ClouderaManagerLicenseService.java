package com.sequenceiq.cloudbreak.cm;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Service
public class ClouderaManagerLicenseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerLicenseService.class);

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private GrpcUmsClient umsClient;

    public void beginTrialIfNeeded(User user, ApiClient apiClient) throws ApiException {
        // Begin the Cloudera Manager trial only if UMS is not enabled. Otherwise, we'll be using a
        // license from UMS.
        if (needTrialLicense(user.getUserCrn())) {
            beginTrial(apiClient);
        } else {
            LOGGER.info("UMS detected and license key available, skipping trial license.");
        }
    }

    private void beginTrial(ApiClient apiClient) throws ApiException {
        LOGGER.info("Enabling trial license.");

        try {
            clouderaManagerClientFactory.getClouderaManagerResourceApi(apiClient).beginTrial();
        } catch (ApiException e) {
            if (trialAlreadyStarted(e)) {
                LOGGER.info("Already had enabled trial license.");
            } else {
                throw e;
            }
        }
    }

    private static boolean trialAlreadyStarted(ApiException e) {
        return e.getCode() == Response.Status.BAD_REQUEST.getStatusCode()
                && e.getResponseBody().contains("Trial has been used.");
    }

    private boolean needTrialLicense(String userCrn) {
        return StringUtils.isEmpty(umsClient.getAccountDetails(userCrn, userCrn, Optional.empty()).getClouderaManagerLicenseKey());
    }

}
