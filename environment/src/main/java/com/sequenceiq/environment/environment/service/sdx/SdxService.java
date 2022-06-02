package com.sequenceiq.environment.environment.service.sdx;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.saas.sdx.PaasRemoteDataContextSupplier;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.sdx.api.endpoint.OperationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;
import com.sequenceiq.sdx.api.model.SdxCcmUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxService implements PaasRemoteDataContextSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    private final SdxEndpoint sdxEndpoint;

    private final SdxUpgradeEndpoint sdxUpgradeEndpoint;

    private final OperationEndpoint sdxOperationEndpoint;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public SdxService(SdxEndpoint sdxEndpoint, SdxUpgradeEndpoint sdxUpgradeEndpoint,
            OperationEndpoint sdxOperationEndpoint, WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {

        this.sdxEndpoint = sdxEndpoint;
        this.sdxUpgradeEndpoint = sdxUpgradeEndpoint;
        this.sdxOperationEndpoint = sdxOperationEndpoint;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
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

    public void startByCrn(String crn) {
        try {
            sdxEndpoint.startByCrn(crn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to start SDX cluster by crn '%s' due to '%s'.", crn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public void stopByCrn(String crn) {
        try {
            sdxEndpoint.stopByCrn(crn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error(String.format("Failed to stop SDX cluster by crn '%s' due to '%s'.", crn, errorMessage), e);
            throw new SdxOperationFailedException(errorMessage, e);
        }
    }

    public SdxCcmUpgradeResponse upgradeCcm(String environmentCrn) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            LOGGER.debug("Calling SDX Upgrade CCM by environment CRN {}", environmentCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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

    @Override
    public Optional<String> getPaasSdxRemoteDataContext(String sdxCrn) {
        throw new IllegalStateException("Environment should not query remote data context for SDX.");
    }
}
