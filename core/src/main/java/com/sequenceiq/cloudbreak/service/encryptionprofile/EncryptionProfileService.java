package com.sequenceiq.cloudbreak.service.encryptionprofile;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.dto.StackDto;
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

    public EncryptionProfileResponse getEncryptionProfileByCrnOrDefault(DetailedEnvironmentResponse environmentResponse, StackDto stackDto) {
        ClusterView clusterView = stackDto.getCluster();
        String encryptionProfileCrn;

        if (StringUtils.isNotBlank(clusterView.getEncryptionProfileCrn())) {
            LOGGER.info("Getting encryption profile {} from cluster", clusterView.getEncryptionProfileCrn());
            encryptionProfileCrn = clusterView.getEncryptionProfileCrn();
        } else {
            encryptionProfileCrn = environmentResponse.getEncryptionProfileCrn();
        }

        return getEncryptionProfileByCrnOrDefaultIfEmpty(encryptionProfileCrn);
    }

    public Optional<EncryptionProfileResponse> getEncryptionProfileByCrn(String encryptionProfileCrn) {
        try {
            return Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByCrn(encryptionProfileCrn)));
        } catch (Exception ex) {
            LOGGER.error("Failed to GET encryption profile by CRN: {}", encryptionProfileCrn, ex);
            return Optional.empty();
        }
    }

    private EncryptionProfileResponse getEncryptionProfileByCrnOrDefaultIfEmpty(String crn) {
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
}
