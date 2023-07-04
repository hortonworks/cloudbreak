package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackV4SecretRotationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidator;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.RedbeamsPoller;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.RotateDatabaseServerSecretV4Request;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

@Service
public class SdxRotationService {

    @Value("${sdx.stack.rotate.sleeptime_sec:20}")
    private int sleepTimeInSec;

    @Value("${sdx.stack.rotate.duration_min:15}")
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
    private SecretRotationValidator secretRotationValidator;

    public void rotateCloudbreakSecret(String datalakeCrn, CloudbreakSecretType secretType, RotationFlowExecutionType executionType) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        StackV4SecretRotationRequest request = new StackV4SecretRotationRequest();
        request.setCrn(datalakeCrn);
        request.setSecret(secretType.name());
        request.setExecutionType(executionType);
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                initiatorUserCrn -> stackV4Endpoint.rotateSecrets(1L, request, initiatorUserCrn)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        cloudbreakPoller.pollFlowStateByFlowIdentifierUntilComplete("secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig);
    }

    public void rotateRedbeamsSecret(String datalakeCrn, RedbeamsSecretType secretType, RotationFlowExecutionType executionType) {
        SdxCluster sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn)
                .orElseThrow(notFound("SdxCluster", datalakeCrn));
        if (sdxCluster.getDatabaseCrn() == null) {
            throw new RuntimeException("No database server found for sdx cluster " + datalakeCrn);
        }

        RotateDatabaseServerSecretV4Request request = new RotateDatabaseServerSecretV4Request();
        request.setCrn(sdxCluster.getDatabaseCrn());
        request.setSecret(secretType.name());
        request.setExecutionType(executionType);

        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                initiatorUserCrn -> databaseServerV4Endpoint.rotateSecret(request, initiatorUserCrn)
        );

        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccurred(true);
        redbeamsPoller.pollFlowStateByFlowIdentifierUntilComplete("secret rotation", flowIdentifier, sdxCluster.getId(), pollingConfig);
    }

    public FlowIdentifier triggerSecretRotation(String datalakeCrn, List<String> secrets, RotationFlowExecutionType executionType) {
        if (entitlementService.isSecretRotationEnabled(Crn.fromString(datalakeCrn).getAccountId())) {
            Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(datalakeCrn);
            if (sdxCluster.isEmpty()) {
                throw new CloudbreakServiceException("No sdx cluster found with crn: " + datalakeCrn);
            } else {
                List<SecretType> secretTypes = secretRotationValidator.mapSecretTypes(secrets, DatalakeSecretType.class);
                return sdxReactorFlowManager.triggerSecretRotation(sdxCluster.get(), secretTypes, executionType);
            }
        } else {
            throw new CloudbreakServiceException("Account is not entitled to execute any secret rotation!");
        }
    }
}
