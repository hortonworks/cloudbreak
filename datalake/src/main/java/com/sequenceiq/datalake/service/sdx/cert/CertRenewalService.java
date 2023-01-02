package com.sequenceiq.datalake.service.sdx.cert;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.cert.renew.event.SdxStartCertRenewalEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class CertRenewalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertRenewalService.class);

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public FlowIdentifier triggerRenewCertificate(SdxCluster sdxCluster, String userCrn) {
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxReactorFlowManager.triggerCertRenewal(new SdxStartCertRenewalEvent(sdxCluster.getId(), userCrn), sdxCluster.getClusterName());
    }

    public FlowIdentifier triggerInternalRenewCertificate(SdxCluster sdxCluster) {
        MDCBuilder.buildMdcContext(sdxCluster);
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return sdxReactorFlowManager.triggerCertRenewal(new SdxStartCertRenewalEvent(sdxCluster.getId(), userCrn, true), sdxCluster.getClusterName());
    }

    public void renewCertificate(SdxCluster sdxCluster, String userCrn) {
        try {
            transactionService.required(() -> {
                try {
                    FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            () -> stackV4Endpoint.renewCertificate(WORKSPACE_ID_DEFAULT, sdxCluster.getClusterName(), userCrn));
                    saveChainIdAndStatus(sdxCluster, flowIdentifier);
                } catch (ClientErrorException e) {
                    try {
                        throw new BadRequestException("Renew certificate failed: " + e.getResponse().readEntity(ExceptionResponse.class).getMessage(), e);
                    } catch (ProcessingException processingException) {
                        LOGGER.warn("Can't read response", processingException);
                        throw e;
                    }
                }
            });
        } catch (TransactionExecutionException e) {
            throw e.getCause();
        }
    }

    public void renewInternalCertificate(SdxCluster sdxCluster) {
        try {
            try {
                FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> stackV4Endpoint.renewInternalCertificate(WORKSPACE_ID_DEFAULT, sdxCluster.getStackCrn()));
                transactionService.required(() -> saveChainIdAndStatus(sdxCluster, flowIdentifier));
            } catch (ClientErrorException e) {
                try {
                    throw new BadRequestException("Renew internal certificate failed: " + e.getResponse().readEntity(ExceptionResponse.class).getMessage(), e);
                } catch (ProcessingException processingException) {
                    LOGGER.warn("Can't read response", processingException);
                    throw e;
                }
            }
        } catch (TransactionExecutionException e) {
            throw e.getCause();
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            throw new RuntimeException(errorMessage);
        }
    }

    private void saveChainIdAndStatus(SdxCluster sdxCluster, FlowIdentifier flowIdentifier) {
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_IN_PROGRESS, "Certificate renewal started",
                sdxCluster.getId());
    }

    public void waitForCloudbreakClusterCertRenewal(Long id, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(id);
        cloudbreakPoller.pollUpdateUntilAvailable("Certificate renewal", sdxCluster, pollingConfig);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_FINISHED, ResourceEvent.DATALAKE_CERT_RENEWAL_FINISHED,
                "Datalake is running", sdxCluster);
    }

    public void finalizeCertRenewal(Long id) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, "Datalake cert renewal finished", id);
    }

    public void handleFailure(Long id, String reason) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_RENEWAL_FAILED,
                defaultIfBlank(reason, "Datalake certificate renewal failed"), id);
    }
}
