package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackV4SecretRotationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.SecretTypeConverter;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationService;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.MultiClusterRotationValidationService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.RedbeamsPoller;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.RedbeamsFlowService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.StateStatus;
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
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Inject
    private RedbeamsPoller redbeamsPoller;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private RedbeamsFlowService redbeamsFlowService;

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private MultiClusterRotationValidationService multiClusterRotationValidationService;

    @Inject
    private MultiClusterRotationService multiClusterRotationService;

    @Inject
    private SecretRotationValidationService secretRotationValidationService;

    public boolean checkOngoingMultiSecretChildrenRotations(String parentCrn, String secret) {
        MultiSecretType multiSecretType = MultiSecretType.valueOf(secret);
        Set<String> crnsByEnvironmentCrn = getSdxCrnsByEnvironmentCrn(parentCrn);
        return CollectionUtils.isNotEmpty(multiClusterRotationService.getMultiRotationEntriesForSecretAndResources(multiSecretType, crnsByEnvironmentCrn));
    }

    public void markMultiClusterChildrenResources(String parentCrn, String secret) {
        Set<String> crnsByEnvironmentCrn = getSdxCrnsByEnvironmentCrn(parentCrn);
        multiClusterRotationService.markChildrenMultiRotationEntriesLocally(crnsByEnvironmentCrn, secret);
    }

    public void rotateCloudbreakSecret(String datalakeCrn, SecretType secretType, RotationFlowExecutionType executionType) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        StackV4SecretRotationRequest request = new StackV4SecretRotationRequest();
        request.setCrn(datalakeCrn);
        request.setSecret(secretType.value());
        request.setExecutionType(executionType);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                initiatorUserCrn -> stackV4Endpoint.rotateSecrets(1L, request, initiatorUserCrn)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        cloudbreakPoller.pollFlowStateByFlowIdentifierUntilComplete("secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig);
    }

    public void rotateRedbeamsSecret(String datalakeCrn, SecretType secretType, RotationFlowExecutionType executionType) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        if (sdxCluster.getDatabaseCrn() == null) {
            throw new RuntimeException("No database server found for sdx cluster " + datalakeCrn);
        }

        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setCrn(sdxCluster.getDatabaseCrn());
        request.setSecret(secretType.value());
        request.setExecutionType(executionType);

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                initiatorUserCrn -> databaseServerV4Endpoint.rotateSecret(request, initiatorUserCrn)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        redbeamsPoller.pollFlowStateByFlowIdentifierUntilComplete("secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig);
    }

    public FlowIdentifier triggerSecretRotation(String datalakeCrn, List<String> secrets, RotationFlowExecutionType requestedExecutionType) {
        if (entitlementService.isSecretRotationEnabled(Crn.fromString(datalakeCrn).getAccountId())) {
            SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn).orElseThrow(notFound("SDX cluster", datalakeCrn));
            List<SecretType> secretTypes = SecretTypeConverter.mapSecretTypes(secrets);
            secretTypes.stream().filter(SecretType::multiSecret).forEach(secretType ->
                    multiClusterRotationValidationService.validateMultiRotationRequest(datalakeCrn, secretType));
            secretRotationValidationService.validateExecutionType(datalakeCrn, secretTypes, requestedExecutionType);
            return sdxReactorFlowManager.triggerSecretRotation(sdxCluster, secretTypes, requestedExecutionType);
        } else {
            throw new CloudbreakServiceException("Account is not entitled to execute any secret rotation!");
        }
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

    private Set<String> getSdxCrnsByEnvironmentCrn(String parentCrn) {
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(
                Crn.safeFromString(parentCrn).getAccountId(), parentCrn).stream().map(SdxCluster::getCrn).collect(Collectors.toSet());
    }
}
