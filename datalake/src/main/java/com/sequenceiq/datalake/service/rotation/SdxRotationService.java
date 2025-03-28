package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.rotation.common.RotationPollingSvcOutageUtils.pollWithSvcOutageErrorHandling;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackV4SecretRotationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.FreeipaPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.RedbeamsPoller;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FreeipaFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.RedbeamsFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;

@Service
public class SdxRotationService {

    @Value("${sdx.stack.rotate.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.rotate.duration_min:30}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private RedbeamsPoller redbeamsPoller;

    @Inject
    private FreeipaPoller freeipaPoller;

    @Inject
    private RedbeamsFlowService redbeamsFlowService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private FreeipaFlowService freeipaFlowService;

    @Inject
    private SecretRotationValidationService secretRotationValidationService;

    @Inject
    private SecretRotationStepProgressService stepProgressService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private List<SecretType> enabledSecretTypes;

    public void rotateCloudbreakSecret(String datalakeCrn, SecretType secretType, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        StackV4SecretRotationRequest request = new StackV4SecretRotationRequest();
        request.setCrn(datalakeCrn);
        request.setSecret(secretType.value());
        request.setExecutionType(executionType);
        request.setAdditionalProperties(additionalProperties);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                initiatorUserCrn -> stackV4Endpoint.rotateSecrets(1L, request, initiatorUserCrn)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        pollWithSvcOutageErrorHandling(
                () -> cloudbreakPoller.pollFlowStateByFlowIdentifierUntilComplete("Secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig),
                        PollerStoppedException.class);
    }

    public void rotateRedbeamsSecret(String datalakeCrn, SecretType secretType, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        if (sdxCluster.getDatabaseCrn() == null) {
            throw new RuntimeException("No database server found for sdx cluster " + datalakeCrn);
        }

        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setCrn(sdxCluster.getDatabaseCrn());
        request.setSecret(secretType.value());
        request.setExecutionType(executionType);
        request.setAdditionalProperties(additionalProperties);

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                initiatorUserCrn -> databaseServerV4Endpoint.rotateSecret(request, initiatorUserCrn)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        pollWithSvcOutageErrorHandling(
                () -> redbeamsPoller.pollFlowStateByFlowIdentifierUntilComplete("Secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig),
                PollerStoppedException.class);
    }

    public FlowIdentifier triggerSecretRotation(String datalakeCrn, List<String> secrets, RotationFlowExecutionType requestedExecutionType,
            Map<String, String> additionalProperties) {
        List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets,
                enabledSecretTypes.stream().map(SecretType::getClass).collect(Collectors.toSet()));
        secretRotationValidationService.validateEnabledSecretTypes(secretTypes, requestedExecutionType);
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn).orElseThrow(notFound("SDX cluster", datalakeCrn));
        SdxStatusEntity status = sdxStatusService.getActualStatusForSdx(sdxCluster.getId());
        Optional<RotationFlowExecutionType> usedExecutionType = secretRotationValidationService.validate(datalakeCrn, secretTypes, requestedExecutionType,
                () -> Set.of(RUNNING, DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED).contains(status.getStatus()));
        return sdxReactorFlowManager.triggerSecretRotation(sdxCluster, secretTypes, usedExecutionType.orElse(null), additionalProperties);
    }

    public void preValidateRedbeamsRotation(String datalakeCrn) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        if (sdxCluster.getDatabaseCrn() == null) {
            throw new SecretRotationException("No database server found for sdx cluster, rotation is not possible.", null);
        }
        FlowLogResponse lastFlow = redbeamsFlowService.getLastFlowId(sdxCluster.getDatabaseCrn());
        if (lastFlow != null && lastFlow.getStateStatus() == StateStatus.PENDING) {
            String message = String.format("Polling in Redbeams is not possible since last known state of flow for the database is %s",
                    lastFlow.getCurrentState());
            throw new SecretRotationException(message, null);
        }
    }

    public void preValidateCloudbreakRotation(String datalakeCrn) {
        FlowLogResponse lastFlow = cloudbreakFlowService.getLastFlowId(datalakeCrn);
        if (lastFlow != null && lastFlow.getStateStatus() == StateStatus.PENDING) {
            String message = String.format("Polling in CB is not possible since last known state of flow for cluster is %s", lastFlow.getCurrentState());
            throw new SecretRotationException(message, null);
        }
    }

    public void cleanupSecretRotationEntries(String datalakeCrn) {
        stepProgressService.deleteAllForResource(datalakeCrn);
    }

    private Set<String> getSdxCrnsByEnvironmentCrn(String parentCrn) {
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(
                Crn.safeFromString(parentCrn).getAccountId(), parentCrn).stream().map(SdxCluster::getCrn).collect(Collectors.toSet());
    }

    public void rotateFreeipaSecret(String datalakeCrn, SecretType secretType, RotationFlowExecutionType executionType,
            Map<String, String> additionalProperties) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));

        FreeIpaSecretRotationRequest request = new FreeIpaSecretRotationRequest();
        request.setSecrets(List.of(secretType.value()));
        request.setExecutionType(executionType);
        request.setAdditionalProperties(additionalProperties);

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                initiatorUserCrn -> freeIpaRotationV1Endpoint.rotateSecretsByCrn(sdxCluster.getEnvCrn(), request)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        pollWithSvcOutageErrorHandling(
                () -> freeipaPoller.pollFlowStateByFlowIdentifierUntilComplete("Secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig),
                PollerStoppedException.class);
    }

    public void preValidateFreeipaRotation(String datalakeCrn) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        FlowLogResponse lastFlow = freeipaFlowService.getLastFlowId(sdxCluster.getEnvCrn());
        if (lastFlow != null && lastFlow.getStateStatus() == StateStatus.PENDING) {
            String message = String.format("Polling in Freeipa is not possible since last known state of flow for FMS is %s", lastFlow.getCurrentState());
            throw new SecretRotationException(message, null);
        }
    }
}
