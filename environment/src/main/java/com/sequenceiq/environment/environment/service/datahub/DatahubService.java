package com.sequenceiq.environment.environment.service.datahub;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.support.SupportV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.support.response.DataHubPlatformSupportRequirements;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXCcmUpgradeV1Response;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class DatahubService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubService.class);

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final DistroXInternalV1Endpoint distroXInternalV1Endpoint;

    private final DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint;

    private final SupportV1Endpoint distroxSupportV1Endpoint;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DatahubService(
            DistroXV1Endpoint distroXV1Endpoint,
            DistroXInternalV1Endpoint distroXInternalV1Endpoint,
            DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint,
            SupportV1Endpoint distroxSupportV1Endpoint,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.distroxSupportV1Endpoint = distroxSupportV1Endpoint;
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.distroXInternalV1Endpoint = distroXInternalV1Endpoint;
        this.distroXUpgradeV1Endpoint = distroXUpgradeV1Endpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public StackViewV4Responses list(String environmentCrn) {
        try {
            return distroXV1Endpoint.list(null, environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to list Datahub clusters for environment '{}' due to: '{}'.", environmentCrn, errorMessage, e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public StackViewV4Responses listInternal(String environmentCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> distroXInternalV1Endpoint.list(environmentCrn),
                    Crn.safeFromString(environmentCrn).getAccountId());
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to list Datahub clusters for environment '{}' due to: '{}'.", environmentCrn, errorMessage, e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public DataHubPlatformSupportRequirements getInstanceTypesByPlatform(String platform) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> distroxSupportV1Endpoint.getInstanceTypesByPlatform(platform)
        );
    }

    public StackV4Response getByCrn(String crn, Set<String> entries) {
        try {
            return distroXV1Endpoint.getByCrn(crn, entries);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get Datahub cluster by crn %s due to: '%s'.", crn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public void putStartByCrns(String environmentCrn, List<String> crns) {
        try {
            distroXV1Endpoint.putStartByCrns(crns);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed start Datahub clusters for environment %s due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public void putStopByCrns(String environmentCrn, List<String> crns) {
        try {
            distroXV1Endpoint.putStopByCrns(crns);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed stop Datahub clusters for environment %s due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public void deleteMultiple(String environmentCrn, DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        try {
            distroXV1Endpoint.deleteMultiple(multiDeleteRequest, forced);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed delete multiple Datahub clusters for environment %s due to: '%s'.", environmentCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }
    }

    public DistroXCcmUpgradeV1Response upgradeCcm(String datahubCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () ->  distroXUpgradeV1Endpoint.upgradeCcmByCrnInternal(datahubCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to Upgrade Cluster Connectivity Manager for Data Hub CRN '%s' due to '%s'.", datahubCrn, errorMessage), e);
            throw new DatahubOperationFailedException(errorMessage, e);
        }

    }

    public FlowIdentifier modifyProxy(String datahubCrn, String previousProxyConfigCrn) {
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> distroXV1Endpoint.modifyProxyInternal(datahubCrn, previousProxyConfigCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to trigger modify proxy config for Data Hub CRN '%s' due to '%s'.", datahubCrn, errorMessage);
            LOGGER.error(message, e);
            throw new DatahubOperationFailedException(message, e);
        }
    }
}
