package com.sequenceiq.datalake.service.sdx.cert;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.cert.rotation.event.SdxStartCertRotationEvent;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class CertRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertRotationService.class);

    @Inject
    private SdxReactorFlowManager flowManager;

    @Inject
    private SdxService sdxService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private CloudbreakPoller statusChecker;

    public FlowIdentifier rotateAutoTlsCertificates(SdxCluster sdxCluster, CertificatesRotationV4Request rotateCertificateRequest) {
        MDCBuilder.buildMdcContext(sdxCluster);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxStartCertRotationEvent event = new SdxStartCertRotationEvent(sdxCluster.getId(), initiatorUserCrn, rotateCertificateRequest);
        return flowManager.triggerCertRotation(event, sdxCluster.getClusterName());
    }

    public void startCertRotation(Long id, CertificatesRotationV4Request request) {
        SdxCluster sdxCluster = sdxService.getById(id);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        try {
            CertificatesRotationV4Response response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.rotateAutoTlsCertificates(0L, sdxCluster.getClusterName(), initiatorUserCrn, request));
            cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, response.getFlowIdentifier());
            updateCertExpirationState(sdxCluster);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_IN_PROGRESS, "Datalake cert rotation in progress", sdxCluster);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Couldn't start certiificate rotation in CB: {}", errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        }
    }

    public void finalizeCertRotation(Long id) {
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING, "Datalake cert rotation finished", id);
    }

    public void handleFailure(Long id, Exception exception) {
        String reason = StringUtils.defaultIfBlank(exception.getMessage(), "Datalake certificate rotation failed");
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_FAILED, reason, id);
    }

    public void waitForCloudbreakClusterCertRotation(Long id, PollingConfig pollingConfig) {
        SdxCluster sdxCluster = sdxService.getById(id);
        statusChecker.pollUpdateUntilAvailable("Certificate rotation", sdxCluster, pollingConfig);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.CERT_ROTATION_FINISHED, ResourceEvent.SDX_CERT_ROTATION_FINISHED,
                "Datalake is running", sdxCluster);
    }

    private void updateCertExpirationState(SdxCluster sdxCluster) {
        sdxService.updateCertExpirationState(sdxCluster.getId(), CertExpirationState.VALID);
        sdxCluster.setCertExpirationState(CertExpirationState.VALID);
    }
}
