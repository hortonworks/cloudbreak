package com.sequenceiq.datalake.service.rotation.certificate;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_CERTIFICATE_ROTATION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_CERTIFICATE_ROTATION_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_CERTIFICATE_ROTATION_NO_DATALAKE;
import static com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType.ERROR;
import static com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType.SKIP;
import static com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType.TRIGGERED;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.StackService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.sdx.api.model.SdxDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertificateV1Response;

@Service
public class SdxDatabaseCertificateRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseCertificateRotationService.class);

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private StackService stackService;

    public SdxRotateRdsCertificateV1Response rotateCertificate(String dlCrn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(dlCrn);
        if (sdxCluster == null) {
            return noDatalakeAnswer(dlCrn);
        }
        stackService.validateRdsSslCertRotation(dlCrn);
        StackV4Response stack = stackService.getDetail(sdxCluster.getClusterName(), null, sdxService.getAccountIdFromCrn(dlCrn));
        return checkPrerequisitesAndTrigger(sdxCluster, stack);
    }

    public StackDatabaseServerCertificateStatusV4Responses getDatabaseCertificateStatus(SdxDatabaseServerCertificateStatusV4Request request) {
        ClusterDatabaseServerCertificateStatusV4Request clusterDatabaseServerCertificateStatusV4Request = new ClusterDatabaseServerCertificateStatusV4Request();
        clusterDatabaseServerCertificateStatusV4Request.setCrns(request.getCrns());
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ClusterDatabaseServerCertificateStatusV4Responses clusterDatabaseServerCertificateStatusV4Responses = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> databaseServerV4Endpoint.listDatabaseServersCertificateStatusByStackCrns(clusterDatabaseServerCertificateStatusV4Request, userCrn)
        );
        Set<StackDatabaseServerCertificateStatusV4Response> responses = new HashSet<>();
        for (ClusterDatabaseServerCertificateStatusV4Response response : clusterDatabaseServerCertificateStatusV4Responses.getResponses()) {
            StackDatabaseServerCertificateStatusV4Response stackDatabaseServerCertificateStatusV4Response = new StackDatabaseServerCertificateStatusV4Response();
            stackDatabaseServerCertificateStatusV4Response.setSslStatus(response.getSslStatus());
            stackDatabaseServerCertificateStatusV4Response.setCrn(response.getCrn());
            responses.add(stackDatabaseServerCertificateStatusV4Response);
        }
        return new StackDatabaseServerCertificateStatusV4Responses(responses);
    }

    public void initAndWaitForStackCertificateRotation(SdxCluster sdxCluster, PollingConfig pollingConfig) {
        String stackCrn = sdxCluster.getStackCrn();
        LOGGER.debug("Initiating Certificate rotation on stack CRN {} for datalake {}", stackCrn, sdxCluster.getName());
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        StackRotateRdsCertificateV4Response stackRotateRdsCertificateV4Response =
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> stackV4Endpoint.rotateRdsCertificateByCrnInternal(0L, stackCrn, initiatorUserCrn));
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, stackRotateRdsCertificateV4Response.getFlowIdentifier());
        LOGGER.debug("Waiting for Certificate rotation on stack CRN {} for datalake {}", stackCrn, sdxCluster.getName());
        cloudbreakPoller.pollCertificateRotationUntilAvailable(sdxCluster, pollingConfig);
    }

    private SdxCluster getSdxClusterByCrn(String dlCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, dlCrn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }

    private SdxRotateRdsCertificateV1Response checkPrerequisitesAndTrigger(SdxCluster sdxCluster, StackV4Response stack) {
        if (Objects.isNull(stack) || !stack.getStatus().isAvailable()) {
            LOGGER.info("Datalake stack {} is not available for Certificate rotation", sdxCluster.getName());
            return getErrorDatalakeAnswer(getMessage(DATALAKE_DATABASE_CERTIFICATE_ROTATION_NOT_AVAILABLE), sdxCluster.getCrn());
        }
        if (!isRemoteDatabaseRequested(stack.getCluster().getDatabaseServerCrn())) {
            LOGGER.info("Datalake stack {} is not using external database", sdxCluster.getName());
            return getErrorDatalakeAnswer(getMessage(DATALAKE_DATABASE_CERTIFICATE_ROTATION_NOT_AVAILABLE), sdxCluster.getCrn());
        }
        LOGGER.info("Triggering flow to rotate RDS client certificates on Datalake {}.", stack.getName());
        return triggerCertificateRotationFlow(sdxCluster);
    }

    private boolean isRemoteDatabaseRequested(String dbServerCrn) {
        return StringUtils.isNotEmpty(dbServerCrn) && Crn.isCrn(dbServerCrn) &&
                CrnResourceDescriptor.DATABASE_SERVER.checkIfCrnMatches(Crn.safeFromString(dbServerCrn));
    }

    private SdxRotateRdsCertificateV1Response getErrorDatalakeAnswer(String message, String stackCrn) {
        return new SdxRotateRdsCertificateV1Response(
                ERROR,
                FlowIdentifier.notTriggered(),
                message,
                stackCrn);
    }

    private SdxRotateRdsCertificateV1Response noDatalakeAnswer(String environmentCrn) {
        LOGGER.debug("Environment {} has no datalake", environmentCrn);
        return new SdxRotateRdsCertificateV1Response(
                SKIP,
                FlowIdentifier.notTriggered(),
                getMessage(DATALAKE_DATABASE_CERTIFICATE_ROTATION_NO_DATALAKE, List.of(environmentCrn)),
                null);
    }

    private SdxRotateRdsCertificateV1Response triggerCertificateRotationFlow(SdxCluster cluster) {
        MDCBuilder.buildMdcContext(cluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerDatabaseCertificateRotation(cluster);
        return new SdxRotateRdsCertificateV1Response(
                TRIGGERED,
                flowIdentifier,
                getMessage(DATALAKE_DATABASE_CERTIFICATE_ROTATION, null),
                cluster.getResourceCrn());
    }

    private String getMessage(ResourceEvent resourceEvent) {
        return messagesService.getMessage(resourceEvent.getMessage());
    }

    private String getMessage(ResourceEvent resourceEvent, List<String> args) {
        return messagesService.getMessage(resourceEvent.getMessage(), args);
    }
}
