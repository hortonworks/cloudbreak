package com.sequenceiq.environment.environment.service.sdx;

import java.util.List;

import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.endpoint.SupportV1Endpoint;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
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

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public SdxService(
            SdxEndpoint sdxEndpoint,
            SdxUpgradeEndpoint sdxUpgradeEndpoint,
            SupportV1Endpoint sdxSupportV1Endpoint,
            SdxInternalEndpoint sdxInternalEndpoint,
            OperationEndpoint sdxOperationEndpoint,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.sdxEndpoint = sdxEndpoint;
        this.sdxUpgradeEndpoint = sdxUpgradeEndpoint;
        this.sdxInternalEndpoint = sdxInternalEndpoint;
        this.sdxOperationEndpoint = sdxOperationEndpoint;
        this.sdxSupportV1Endpoint = sdxSupportV1Endpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public SdxClusterResponse getByCrn(String clusterCrn) {
        try {
            return sdxEndpoint.getByCrn(clusterCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get SDX cluster by crn '%s' due to '%s'.", clusterCrn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public List<SdxClusterResponse> list(String envName) {
        try {
            return sdxEndpoint.list(envName, false);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to list SDX clusters by environment name '%s' due to '%s'.", envName, errorMessage), e);
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
            LOGGER.warn(String.format(
                    "Failed to check if SDX cluster with crn '%s' has unstoppable flow due to '%s'.", crn, errorMessage
            ), e);
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
            LOGGER.debug("Calling SDX Upgrade CCM by environment CRN {}", environmentCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> sdxUpgradeEndpoint.upgradeCcm(environmentCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to Upgrade Cluster Connectivity Manager by environment CRN '%s' due to '%s'.", environmentCrn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public OperationView getOperation(String datalakeCrn, boolean detailed) {
        try {
            LOGGER.debug("Calling SDX Operation result for datalake CRN {}", datalakeCrn);
            return sdxOperationEndpoint.getOperationProgressByResourceCrn(datalakeCrn, detailed);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to get Operation for datalake CRN '%s' due to '%s'.", datalakeCrn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public FlowIdentifier modifyProxy(String datalakeCrn, String previousProxyCrn) {
        try {
            LOGGER.debug("Calling SDX modify proxy by CRN {}", datalakeCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    initiatorUserCrn -> sdxInternalEndpoint.modifyProxy(datalakeCrn, previousProxyCrn, initiatorUserCrn));
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to modify proxy config of SDX cluster by crn '%s' due to '%s'.", datalakeCrn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }
}
