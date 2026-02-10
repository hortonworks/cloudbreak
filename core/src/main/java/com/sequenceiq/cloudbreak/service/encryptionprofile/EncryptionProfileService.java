package com.sequenceiq.cloudbreak.service.encryptionprofile;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
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

    @Inject
    private ClusterService clusterService;

    public String getEncryptionProfileByCrnOrDefault(DetailedEnvironmentResponse environmentResponse, StackDto stackDto) {
        ClusterView clusterView = stackDto.getCluster();
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

    public void setEncryptionProfile(String encryptionProfileCrn, Stack stack) {
        if (StringUtils.isNotEmpty(encryptionProfileCrn)) {
            EncryptionProfileResponse encryptionProfile = getEncryptionProfileByCrn(encryptionProfileCrn);
            Cluster cluster = stack.getCluster();
            cluster.setEncryptionProfileCrn(encryptionProfile.getCrn());
            clusterService.save(cluster);
        } else {
            LOGGER.info("No custom encryption profile for cluster. Cluster will use the environment's encryption profile.");
        }
    }

    public EncryptionProfileResponse getEncryptionProfileOrThrowException(String encryptionProfileNameOrCrn) {
        EncryptionProfileResponse encryptionProfile;
        if (Crn.isCrn(encryptionProfileNameOrCrn)) {
            encryptionProfile = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByCrn(encryptionProfileNameOrCrn));
        } else {
            encryptionProfile = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> encryptionProfileEndpoint.getByName(encryptionProfileNameOrCrn));
        }

        if (encryptionProfile == null) {
            throw new NotFoundException("Encryption profile not found: " + encryptionProfileNameOrCrn);
        }

        return encryptionProfile;
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

    public Optional<String> getDefaultEncryptionProfileIfRequired(
            DetailedEnvironmentResponse environment,
            Cluster cluster,
            Optional<String> runtimeVersion
    ) {
        if (StringUtils.isNoneBlank(cluster.getEncryptionProfileCrn())) {
            return Optional.ofNullable(cluster.getEncryptionProfileCrn());
        } else if (govCloudAnd732(environment, runtimeVersion)) {
            return Optional.ofNullable(getDefaultEncryptionProfile().getCrn());
        }
        return Optional.empty();
    }

    private boolean govCloudAnd732(DetailedEnvironmentResponse environment, Optional<String> runtimeVersion) {
        return environment.getCredential().getGovCloud()
                && isVersionNewerOrEqualThanLimited(runtimeVersion.get(), CLOUDERA_STACK_VERSION_7_3_2);
    }
}
