package com.sequenceiq.datalake.service.rotation.certificate;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_CERTIFICATE_ROTATION;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_CERTIFICATE_ROTATION_NOT_AVAILABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_DATABASE_CERTIFICATE_ROTATION_NO_DATALAKE;
import static com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType.ERROR;
import static com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType.SKIP;
import static com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertResponseType.TRIGGERED;

import java.util.HashSet;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslCertificateEntryResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.support.SupportV4Endpoint;
import com.sequenceiq.sdx.api.model.rotaterdscert.SdxRotateRdsCertificateV1Response;

@Service
public class SdxDatabaseCertificateRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDatabaseCertificateRotationService.class);

    @Inject
    private EnvironmentClientService environmentService;

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
    private DistroXV1Endpoint distroXV1Endpoint;

    @Inject
    private DistroXDatabaseServerV1Endpoint distroXDatabaseServerV1Endpoint;

    @Inject
    private SupportV4Endpoint supportV4Endpoint;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public SdxRotateRdsCertificateV1Response rotateCertificate(String dlCrn) {
        SdxCluster sdxCluster = getSdxClusterByCrn(dlCrn);
        if (sdxCluster == null) {
            return noDatalakeAnswer(dlCrn);
        }
        StackV4Response stack = sdxService.getDetail(sdxCluster.getClusterName(), null, sdxService.getAccountIdFromCrn(dlCrn));
        return checkPrerequisitesAndTrigger(sdxCluster, stack);
    }

    public void initAndWaitForStackCertificateRotation(SdxCluster sdxCluster, PollingConfig pollingConfig) {
        String stackCrn = sdxCluster.getStackCrn();
        LOGGER.debug("Initiating Certificate rotation on stack CRN {} for datalake {}", stackCrn, sdxCluster.getName());
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        StackRotateRdsCertificateV4Response stackRotateRdsCertificateV4Response =
                ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> stackV4Endpoint.rotateRdsCertificateByCrnInternal(0L, stackCrn, initiatorUserCrn));
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, stackRotateRdsCertificateV4Response.getFlowIdentifier());
        LOGGER.debug("Waiting for Certificate rotation on stack CRN {} for datalake {}", stackCrn, sdxCluster.getName());
        cloudbreakPoller.pollDatabaseCertificateRotationUntilAvailable(sdxCluster, pollingConfig);
    }

    private SdxCluster getSdxClusterByCrn(String dlCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, dlCrn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }

    private SdxRotateRdsCertificateV1Response checkPrerequisitesAndTrigger(SdxCluster sdxCluster, StackV4Response stack) {
        if (!stack.getStatus().isAvailable()) {
            LOGGER.debug("Datalake stack {} is not available for Certificate rotation", stack.getName());
            return new SdxRotateRdsCertificateV1Response(
                    ERROR,
                    FlowIdentifier.notTriggered(),
                    getMessage(DATALAKE_DATABASE_CERTIFICATE_ROTATION_NOT_AVAILABLE),
                    stack.getCrn());
        }
        StackViewV4Responses stackViewV4Responses = distroXV1Endpoint.list(null, stack.getEnvironmentCrn());
        SslCertificateEntryResponse latestCertificates = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> supportV4Endpoint.getLatestCertificates(stack.getCloudPlatform().name(), stack.getRegion()));
        for (StackViewV4Response response : stackViewV4Responses.getResponses()) {
            if (!response.getExternalDatabase().getAvailabilityType().isEmbedded()) {
                StackDatabaseServerResponse databaseServerByCrn = distroXDatabaseServerV1Endpoint.getDatabaseServerByCrn(response.getCrn());
                if (databaseServerByCrn.getSslConfig().getSslCertificateActiveVersion() != latestCertificates.getVersion()) {
                    return new SdxRotateRdsCertificateV1Response(
                            ERROR,
                            FlowIdentifier.notTriggered(),
                            String.format("Data Hub %s is not on the latest certificate version. " +
                                    "Please update that Data Hub first before update the Data Lake", response.getName()),
                            stack.getCrn());
                }
            } else {
                StackV4Response byCrn = distroXV1Endpoint.getByCrn(response.getCrn(), new HashSet<>());
                if (!byCrn.getCluster().getDbSslRootCertBundle().contains(latestCertificates.getCertPem())) {
                    return new SdxRotateRdsCertificateV1Response(
                            ERROR,
                            FlowIdentifier.notTriggered(),
                            String.format("Data Hub %s is not on the latest certificate version. " +
                                    "Please update that Data Hub first before update the Data Lake", response.getName()),
                            stack.getCrn());
                }
            }
        }
        LOGGER.debug("Datalake stack {} has to be Certificate rotated.", stack.getName());
        return triggerCertificateRotationFlow(sdxCluster);
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
