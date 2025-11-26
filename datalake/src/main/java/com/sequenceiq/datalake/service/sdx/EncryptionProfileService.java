package com.sequenceiq.datalake.service.sdx;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@Service
public class EncryptionProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionProfileService.class);

    private static final String DEFAULT_ENCRYPTION_PROFILE_NAME = "cdp_default";

    @Inject
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private SdxVersionRuleEnforcer sdxVersionRuleEnforcer;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public EncryptionProfileResponse getCrn(String crn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByCrn(crn));

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

    public Optional<EncryptionProfileResponse> getEncryptionProfileFromDatalakeOtherwiseFromEnv(String encryptionProfileCrnFromEnv,
            String encryptionProfileCrnFromCluster) {
        String encryptionProfileCrn;

        if (StringUtils.isNotBlank(encryptionProfileCrnFromCluster)) {
            LOGGER.info("Getting encryption profile {} from cluster", encryptionProfileCrnFromCluster);
            encryptionProfileCrn = encryptionProfileCrnFromCluster;
        } else {
            encryptionProfileCrn = encryptionProfileCrnFromEnv;
        }

        try {
            return Optional.ofNullable(getCrn(encryptionProfileCrn));

        } catch (Exception ex) {
            LOGGER.error("Encryption Profile CRN {} not found", encryptionProfileCrn, ex);
            return Optional.empty();
        }
    }

    public void validateEncryptionProfile(SdxClusterRequest clusterRequest, DetailedEnvironmentResponse environment) {
        ValidationResult.ValidationResultBuilder validationBuilder = new ValidationResult.ValidationResultBuilder();

        if (checkIfEncryptionProfileCrnIsNeitherNullOrDefault(environment.getEncryptionProfileCrn()) ||
                checkIfEncryptionProfileCrnIsNeitherNullOrDefault(clusterRequest.getEncryptionProfileCrn())) {

            if (!entitlementService.isConfigureEncryptionProfileEnabled(environment.getAccountId())) {
                validationBuilder.error("Encryption Profile entitlement is not granted to the account");
            }

            if (!sdxVersionRuleEnforcer.isCustomEncryptionProfileSupported(clusterRequest.getRuntime())) {
                validationBuilder.error(format("Encryption Profile is not supported in %s runtime. Please use 7.3.2 or above",
                        clusterRequest.getRuntime()));
            }

            Optional<EncryptionProfileResponse> encryptionProfileResponseOp = getEncryptionProfileFromDatalakeOtherwiseFromEnv(
                    environment.getEncryptionProfileCrn(), clusterRequest.getEncryptionProfileCrn());

            if (encryptionProfileResponseOp.isEmpty()) {
                validationBuilder.error("Encryption Profile not found");
            }

            ValidationResult validationResult = validationBuilder.build();
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    private boolean checkIfEncryptionProfileCrnIsNeitherNullOrDefault(String crn) {
        if (isNotEmpty(crn)) {
            String encryptionProfileName = Crn.safeFromString(crn).getResource();
            return !encryptionProfileName.startsWith(DEFAULT_ENCRYPTION_PROFILE_NAME);
        }
        return false;
    }
}
