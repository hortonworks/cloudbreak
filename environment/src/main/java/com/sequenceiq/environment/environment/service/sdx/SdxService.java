package com.sequenceiq.environment.environment.service.sdx;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateTrustedRealmRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEncryptionProfileEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.endpoint.SupportV1Endpoint;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxStopValidationResponse;
import com.sequenceiq.sdx.api.model.support.DatalakePlatformSupportRequirements;

@Service
public class SdxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    private final SdxEndpoint sdxEndpoint;

    private final SdxUpgradeEndpoint sdxUpgradeEndpoint;

    private final SdxInternalEndpoint sdxInternalEndpoint;

    private final SupportV1Endpoint sdxSupportV1Endpoint;

    private final OperationEndpoint sdxOperationEndpoint;

    private final SdxEncryptionProfileEndpoint sdxEncryptionProfileEndpoint;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public SdxService(
            SdxEndpoint sdxEndpoint,
            SdxUpgradeEndpoint sdxUpgradeEndpoint,
            SupportV1Endpoint sdxSupportV1Endpoint,
            SdxInternalEndpoint sdxInternalEndpoint,
            OperationEndpoint sdxOperationEndpoint,
            SdxEncryptionProfileEndpoint sdxEncryptionProfileEndpoint,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.sdxEndpoint = sdxEndpoint;
        this.sdxUpgradeEndpoint = sdxUpgradeEndpoint;
        this.sdxInternalEndpoint = sdxInternalEndpoint;
        this.sdxOperationEndpoint = sdxOperationEndpoint;
        this.sdxSupportV1Endpoint = sdxSupportV1Endpoint;
        this.sdxEncryptionProfileEndpoint = sdxEncryptionProfileEndpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public SdxClusterResponse getByCrn(String clusterCrn) {
        try {
            return sdxEndpoint.getByCrn(clusterCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get Data Lake cluster by crn '{}' due to '{}'.", clusterCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public SdxClusterDetailResponse getDetailByCrn(String clusterCrn) {
        try {
            return sdxEndpoint.getDetailByCrn(clusterCrn, Set.of());
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get Data Lake cluster detail by crn '{}' due to '{}'.", clusterCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public List<SdxClusterResponse> list(String envName) {
        try {
            return sdxEndpoint.list(envName, false);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to list Data Lake clusters by environment name '{}' due to '{}'.", envName, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public List<SdxClusterResponse> listByEnvironmentCrn(String environmentCrn) {
        try {
            return sdxEndpoint.getByEnvCrn(environmentCrn, false);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to list Data Lake clusters by environment CRN '{}' due to '{}'.", environmentCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public SdxStopValidationResponse isStoppable(String crn) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> sdxEndpoint.isStoppableInternal(crn, initiatorUserCrn)
            );
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.warn("Failed to check if Data Lake cluster with crn '{}' has unstoppable flow due to '{}'.", crn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public DatalakePlatformSupportRequirements getInstanceTypesByPlatform(String platform) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> sdxSupportV1Endpoint.getInstanceTypesByPlatform(platform)
        );
    }

    public SdxCcmUpgradeResponse upgradeCcm(String environmentCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            LOGGER.debug("Calling Data Lake Upgrade CCM by environment CRN {}", environmentCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> sdxUpgradeEndpoint.upgradeCcm(environmentCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to Upgrade Cluster Connectivity Manager by environment CRN '{}' due to '{}'.", environmentCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public OperationView getOperation(String datalakeCrn, boolean detailed) {
        try {
            LOGGER.debug("Calling Data Lake Operation result for Data Lake CRN {}", datalakeCrn);
            return sdxOperationEndpoint.getOperationProgressByResourceCrn(datalakeCrn, detailed);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get Operation for Data Lake CRN '{}' due to '{}'.", datalakeCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public FlowIdentifier modifyProxy(String datalakeCrn, String previousProxyCrn) {
        try {
            LOGGER.debug("Calling Data Lake modify proxy by CRN {}", datalakeCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> sdxInternalEndpoint.modifyProxy(datalakeCrn, previousProxyCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to modify proxy config of Data Lake cluster by crn '{}' due to '{}'.", datalakeCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public FlowIdentifier updateTrustedRealm(String datalakeCrn, UpdateTrustedRealmRequest request) {
        try {
            LOGGER.debug("Calling Data Lake update trusted realm by CRN {}", datalakeCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> sdxInternalEndpoint.updateTrustedRealm(datalakeCrn, request, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to update trusted realm on Data Lake cluster by crn '{}' due to '{}'.", datalakeCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public FlowIdentifier updateSalt(String datalakeCrn) {
        try {
            LOGGER.debug("Calling Data Lake salt update by CRN {}", datalakeCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> sdxInternalEndpoint.updateSalt(datalakeCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to update salt on Data Lake cluster by crn '{}' due to '{}'.", datalakeCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public FlowIdentifier enableEncryptionProfile(String datalakeCrn, String encryptionProfileCrn) {
        try {
            LOGGER.debug("Calling Data Lake enable encryption profile by Data Lake CRN {}", datalakeCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> sdxEncryptionProfileEndpoint.enableEncryptionProfileByCrn(datalakeCrn, encryptionProfileCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to enable encryption profile on Data Lake cluster by crn '{}' due to '{}'.", datalakeCrn, errorMessage, e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }
}
