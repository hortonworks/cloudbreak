package com.sequenceiq.datalake.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static com.sequenceiq.sdx.rotation.DatalakeSecretType.DATALAKE_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.CloudbreakPoller;
import com.sequenceiq.datalake.service.sdx.FreeipaPoller;
import com.sequenceiq.datalake.service.sdx.RedbeamsPoller;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FreeipaFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.RedbeamsFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.api.model.StateStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaRotationV1Endpoint;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.rotation.RedbeamsSecretType;

@ExtendWith(MockitoExtension.class)
class SdxRotationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:datalake:1";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    private static final Long SDX_CLUSTER_ID = 10L;

    private static final String DATABASE_CRN = "databaseCrn";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:envCrn1";

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private FreeIpaRotationV1Endpoint freeIpaRotationV1Endpoint;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private CloudbreakPoller cloudbreakPoller;

    @Mock
    private RedbeamsPoller redbeamsPoller;

    @Mock
    private FreeipaPoller freeipaPoller;

    @Mock
    private RedbeamsFlowService redbeamsFlowService;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private FreeipaFlowService freeipaFlowService;

    @Mock
    private SecretRotationValidationService secretRotationValidationService;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private SdxRotationService underTest;

    @Test
    void rotateCloudbreakSecretShouldSucceed() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_CLUSTER_ID);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.of(sdxCluster));
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_CHAIN_ID);
        when(stackV4Endpoint.rotateSecrets(eq(1L), any(), any())).thenReturn(flowIdentifier);
        underTest.rotateCloudbreakSecret(RESOURCE_CRN, DATALAKE_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null);
        verify(sdxClusterRepository, times(1)).findByCrnAndDeletedIsNull(eq(RESOURCE_CRN));
        verify(regionAwareInternalCrnGeneratorFactory, times(1)).iam();
        verify(stackV4Endpoint, times(1)).rotateSecrets(eq(1L), any(), any());
        verify(cloudbreakPoller, times(1))
                .pollFlowStateByFlowIdentifierUntilComplete(eq("Secret rotation"), eq(flowIdentifier), eq(SDX_CLUSTER_ID), any());
    }

    @Test
    void rotateRedbeamsSecretShouldSucceed() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_CLUSTER_ID);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn(DATABASE_CRN);
        sdxCluster.setSdxDatabase(sdxDatabase);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.of(sdxCluster));
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_CHAIN_ID);
        when(databaseServerV4Endpoint.rotateSecret(any(), any())).thenReturn(flowIdentifier);
        underTest.rotateRedbeamsSecret(RESOURCE_CRN, RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null);
        verify(sdxClusterRepository, times(1)).findByCrnAndDeletedIsNull(eq(RESOURCE_CRN));
        verify(regionAwareInternalCrnGeneratorFactory, times(1)).iam();
        verify(databaseServerV4Endpoint, times(1)).rotateSecret(any(), any());
        verify(redbeamsPoller, times(1))
                .pollFlowStateByFlowIdentifierUntilComplete(eq("Secret rotation"), eq(flowIdentifier), eq(SDX_CLUSTER_ID), any());
    }

    @Test
    void rotateFreeipaSecretShouldSucceed() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_CLUSTER_ID);
        sdxCluster.setEnvCrn(ENV_CRN);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.of(sdxCluster));
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator = mock(RegionAwareInternalCrnGenerator.class);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("internalCrn");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW_CHAIN, FLOW_CHAIN_ID);
        when(freeIpaRotationV1Endpoint.rotateSecretsByCrn(any(), any())).thenReturn(flowIdentifier);
        underTest.rotateFreeipaSecret(RESOURCE_CRN, FreeIpaSecretType.FREEIPA_KERBEROS_BIND_USER, ROTATE, null);
        verify(sdxClusterRepository, times(1)).findByCrnAndDeletedIsNull(eq(RESOURCE_CRN));
        verify(regionAwareInternalCrnGeneratorFactory, times(1)).iam();
        verify(freeIpaRotationV1Endpoint, times(1)).rotateSecretsByCrn(any(), any());
        verify(freeipaPoller, times(1))
                .pollFlowStateByFlowIdentifierUntilComplete(eq("Secret rotation"), eq(flowIdentifier), eq(SDX_CLUSTER_ID), any());
    }

    @Test
    void rotateRedbeamsSecretShouldFailIfSdxClusterNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.rotateRedbeamsSecret(RESOURCE_CRN, RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null));
        verify(sdxClusterRepository, times(1)).findByCrnAndDeletedIsNull(eq(RESOURCE_CRN));
        assertEquals("SdxCluster '" + RESOURCE_CRN + "' not found.", notFoundException.getMessage());
    }

    @Test
    void rotateFreeipaSecretShouldFailIfSdxClusterNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.rotateFreeipaSecret(RESOURCE_CRN, FreeIpaSecretType.FREEIPA_KERBEROS_BIND_USER, ROTATE, null));
        verify(sdxClusterRepository, times(1)).findByCrnAndDeletedIsNull(eq(RESOURCE_CRN));
        assertEquals("SdxCluster '" + RESOURCE_CRN + "' not found.", notFoundException.getMessage());
    }

    @Test
    void rotateRedbeamsSecretShouldFailIfDatabaseCrnNotFound() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setSdxDatabase(new SdxDatabase());
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.of(sdxCluster));
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
                () -> underTest.rotateRedbeamsSecret(RESOURCE_CRN, RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD, ROTATE, null));
        verify(sdxClusterRepository, times(1)).findByCrnAndDeletedIsNull(eq(RESOURCE_CRN));
        assertEquals("No database server found for sdx cluster " + RESOURCE_CRN, runtimeException.getMessage());
    }

    @Test
    void triggerSecretRotationShouldSucceed() {
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.empty());
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(anyLong())).thenReturn(status);
        FlowIdentifier flowIdentifier = underTest.triggerSecretRotation(RESOURCE_CRN, List.of(DATALAKE_DATABASE_ROOT_PASSWORD.name()), null, null);
        verify(sdxReactorFlowManager, times(1)).triggerSecretRotation(eq(sdxCluster), anyList(), isNull(), any());
        verify(secretRotationValidationService, times(1)).validateEnabledSecretTypes(eq(List.of(DATALAKE_DATABASE_ROOT_PASSWORD)), isNull());
    }

    @Test
    void triggerSecretRotationShouldSucceedIfRollbackFinished() {
        when(secretRotationValidationService.validate(any(), any(), any(), any())).thenReturn(Optional.empty());
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(1L);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        SdxStatusEntity status = new SdxStatusEntity();
        status.setStatus(DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FINISHED);
        when(sdxStatusService.getActualStatusForSdx(anyLong())).thenReturn(status);
        underTest.triggerSecretRotation(RESOURCE_CRN, List.of(DATALAKE_DATABASE_ROOT_PASSWORD.name()), null, null);
        verify(sdxReactorFlowManager, times(1)).triggerSecretRotation(eq(sdxCluster), anyList(), isNull(), any());
        verify(secretRotationValidationService, times(1)).validateEnabledSecretTypes(eq(List.of(DATALAKE_DATABASE_ROOT_PASSWORD)), isNull());
    }

    @Test
    void triggerSecretRotationShouldFailIfSdxClusterNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.triggerSecretRotation(RESOURCE_CRN, List.of(DATALAKE_DATABASE_ROOT_PASSWORD.name()), null, null));
        assertEquals("SDX cluster '" + RESOURCE_CRN + "' not found.", notFoundException.getMessage());
        verify(secretRotationValidationService, times(1)).validateEnabledSecretTypes(eq(List.of(DATALAKE_DATABASE_ROOT_PASSWORD)), isNull());
    }

    @Test
    void preValidateRedbeamsRotationShouldFailIfSdxClusterNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.preValidateRedbeamsRotation(RESOURCE_CRN));
        assertEquals("SdxCluster 'crn:cdp:datalake:us-west-1:1234:datalake:1' not found.", notFoundException.getMessage());
        verify(redbeamsFlowService, never()).getLastFlowId(anyString());
    }

    @Test
    void preValidateFreeipaRotationShouldFailIfSdxClusterNotFound() {
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(eq(RESOURCE_CRN))).thenReturn(Optional.empty());
        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> underTest.preValidateRedbeamsRotation(RESOURCE_CRN));
        assertEquals("SdxCluster 'crn:cdp:datalake:us-west-1:1234:datalake:1' not found.", notFoundException.getMessage());
        verify(freeipaFlowService, never()).getLastFlowId(anyString());
    }

    @Test
    void preValidateRedbeamsRotationShouldFailDatabaseCrnIsEmpty() {
        SdxCluster sdxCluster = new SdxCluster();
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxCluster.setSdxDatabase(sdxDatabase);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidateRedbeamsRotation(RESOURCE_CRN));
        assertEquals("No database server found for sdx cluster, rotation is not possible.", secretRotationException.getMessage());
        verify(redbeamsFlowService, never()).getLastFlowId(anyString());
    }

    @Test
    void preValidateRedbeamsRotationShouldFailIfRedbeamsFlowIsRunning() {
        SdxCluster sdxCluster = new SdxCluster();
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn(DATABASE_CRN);
        sdxCluster.setSdxDatabase(sdxDatabase);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.PENDING);
        lastFlow.setCurrentState("currentState");
        when(redbeamsFlowService.getLastFlowId(eq(DATABASE_CRN))).thenReturn(lastFlow);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidateRedbeamsRotation(RESOURCE_CRN));
        assertEquals("Polling in Redbeams is not possible since last known state of flow for the database is currentState",
                secretRotationException.getMessage());
        verify(redbeamsFlowService, times(1)).getLastFlowId(eq(DATABASE_CRN));
    }

    @Test
    void preValidateFreeipaRotationShouldFailIfRedbeamsFlowIsRunning() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvCrn(ENV_CRN);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.PENDING);
        lastFlow.setCurrentState("currentState");
        when(freeipaFlowService.getLastFlowId(eq(ENV_CRN))).thenReturn(lastFlow);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidateFreeipaRotation(RESOURCE_CRN));
        assertEquals("Polling in Freeipa is not possible since last known state of flow for FMS is currentState",
                secretRotationException.getMessage());
        verify(freeipaFlowService, times(1)).getLastFlowId(eq(ENV_CRN));
    }

    @Test
    void preValidateRedbeamsRotationShouldSucceedIfRedbeamsFlowIsNotRunning() {
        SdxCluster sdxCluster = new SdxCluster();
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn(DATABASE_CRN);
        sdxCluster.setSdxDatabase(sdxDatabase);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.SUCCESSFUL);
        when(redbeamsFlowService.getLastFlowId(eq(DATABASE_CRN))).thenReturn(lastFlow);
        underTest.preValidateRedbeamsRotation(RESOURCE_CRN);
        verify(redbeamsFlowService, times(1)).getLastFlowId(eq(DATABASE_CRN));
    }

    @Test
    void preValidateFreeipaRotationShouldSucceedIfRedbeamsFlowIsNotRunning() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvCrn(ENV_CRN);
        when(sdxClusterRepository.findByCrnAndDeletedIsNull(RESOURCE_CRN)).thenReturn(Optional.of(sdxCluster));
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.SUCCESSFUL);
        when(freeipaFlowService.getLastFlowId(eq(ENV_CRN))).thenReturn(lastFlow);
        underTest.preValidateFreeipaRotation(RESOURCE_CRN);
        verify(freeipaFlowService, times(1)).getLastFlowId(eq(ENV_CRN));
    }

    @Test
    void preValidateCloudbreakRotationShouldFailIfCloudbreakFlowIsRunning() {
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.PENDING);
        lastFlow.setCurrentState("currentState");
        when(cloudbreakFlowService.getLastFlowId(eq(RESOURCE_CRN))).thenReturn(lastFlow);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.preValidateCloudbreakRotation(RESOURCE_CRN));
        assertEquals("Polling in CB is not possible since last known state of flow for cluster is currentState",
                secretRotationException.getMessage());
        verify(cloudbreakFlowService, times(1)).getLastFlowId(eq(RESOURCE_CRN));
    }

    @Test
    void preValidateCloudberakRotationShouldSucceedIfCloudbreakFlowIsNotRunning() {
        FlowLogResponse lastFlow = new FlowLogResponse();
        lastFlow.setStateStatus(StateStatus.SUCCESSFUL);
        when(cloudbreakFlowService.getLastFlowId(eq(RESOURCE_CRN))).thenReturn(lastFlow);
        underTest.preValidateCloudbreakRotation(RESOURCE_CRN);
        verify(cloudbreakFlowService, times(1)).getLastFlowId(eq(RESOURCE_CRN));
    }
}