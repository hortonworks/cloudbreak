package com.sequenceiq.cloudbreak.service.encryptionprofile;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class EncryptionProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileService.class);

    @Inject
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private EntitlementService entitlementService;

    public String getEncryptionProfileCrn(DetailedEnvironmentResponse environmentResponse, ClusterView clusterView) {
        String encryptionProfileCrn;

        if (StringUtils.isNotBlank(clusterView.getEncryptionProfileCrn())) {
            LOGGER.info("Getting encryption profile {} from cluster", clusterView.getEncryptionProfileCrn());
            encryptionProfileCrn = clusterView.getEncryptionProfileCrn();
        } else {
            encryptionProfileCrn = environmentResponse.getEncryptionProfileCrn();
        }

        return encryptionProfileCrn;
    }

    public EncryptionProfileResponse getEncryptionProfileByCrn(String encryptionProfileCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByCrn(encryptionProfileCrn));
        } catch (Exception ex) {
            LOGGER.error("Failed to GET encryption profile by CRN: {}", encryptionProfileCrn, ex);
            throw new CloudbreakServiceException(ex.getMessage(), ex);
        }
    }

    public EncryptionProfileResponse getEncryptionProfileByCrnOrDefault(String crn) {
        try {
            if (StringUtils.isNotEmpty(crn)) {
                return ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> encryptionProfileEndpoint.getByCrn(crn));
            } else {
                return getDefaultEncryptionProfile();
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to GET encryption profile by CRN: %s, due to: %s. %s.", crn, e.getMessage(), errorMessage);
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (Exception e) {
            String message = String.format("Failed to GET encryption profile by CRN: %s, due to: '%s' ", crn, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }

    private EncryptionProfileResponse getDefaultEncryptionProfile() {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> encryptionProfileEndpoint.getDefaultEncryptionProfile());
    }

    public EncryptionProfileResponse getEncryptionProfileByName(String encryptionProfileName) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByName(encryptionProfileName));
        } catch (Exception ex) {
            LOGGER.error("Failed to GET encryption profile by name: {}", encryptionProfileName, ex);
            throw new CloudbreakServiceException(webApplicationExceptionMessageExtractor.getErrorMessage(ex));
        }
    }

    public EncryptionProfileResponse getEncryptionProfileByNameOrCrn(String encryptionProfileNameOrCrn, String encryptionProfileCrn) {
        EncryptionProfileResponse encryptionProfile = null;
        if (entitlementService.isConfigureEncryptionProfileEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            String nameOrCrn = StringUtils.isNotBlank(encryptionProfileNameOrCrn) ?
                    encryptionProfileNameOrCrn : encryptionProfileCrn;
            if (StringUtils.isNotEmpty(nameOrCrn)) {
                if (Crn.isCrn(nameOrCrn)) {
                    encryptionProfile = getEncryptionProfileByCrn(nameOrCrn);
                } else {
                    encryptionProfile = getEncryptionProfileByName(nameOrCrn);
                }
            }
        }
        return encryptionProfile;
    }
}
