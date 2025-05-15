package com.sequenceiq.datalake.service.sdx.dr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.BackupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.RestoreV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrClient;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationStatus;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.flowcheck.FlowState;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.DatalakeDatabaseDrStatus;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreStatusResponse;

@ExtendWith(MockitoExtension.class)
public class SdxBackupRestoreServiceTest {

    private static final String BACKUP_ID = UUID.randomUUID().toString();

    private static final String RESTORE_ID = UUID.randomUUID().toString();

    private static final String BACKUP_LOCATION = "location/of/backup";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1:user:2";

    private static final String BACKUP_NAME = "backup_02";

    private static final String POLLABLE_ID = "datalake-dr-flow";

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);

    private static final String CLUSTER_NAME = "test-sdx-cluster";

    private static final String RUNTIME = "7.2.2";

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxOperationRepository sdxOperationRepository;

    @Mock
    private DatalakeDrClient datalakeDrClient;

    @Mock
    private SdxClusterRepository sdxClusterRepository;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private DatalakeDrConfig datalakeDrConfig;

    @Mock
    private CloudbreakFlowService cloudbreakFlowService;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private SdxBackupRestoreService underTest;

    private SdxCluster sdxCluster;

    @BeforeEach
    public void initialize() {
        sdxCluster = getValidSdxCluster();
    }

    @Test
    public void triggerDatabaseBackupSuccess() {
        when(sdxReactorFlowManager.triggerDatalakeDatabaseBackupFlow(any(DatalakeDatabaseBackupStartEvent.class), anyString())).thenReturn(FLOW_IDENTIFIER);
        SdxDatabaseBackupRequest backupRequest = new SdxDatabaseBackupRequest();
        backupRequest.setBackupId(BACKUP_ID);
        backupRequest.setBackupLocation(BACKUP_LOCATION);
        backupRequest.setCloseConnections(true);
        SdxDatabaseBackupResponse backupResponse = underTest.triggerDatabaseBackup(sdxCluster, backupRequest);
        assertEquals(FLOW_IDENTIFIER, backupResponse.getFlowIdentifier());
        ArgumentCaptor<DatalakeDatabaseBackupStartEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DatalakeDatabaseBackupStartEvent.class);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseBackupFlow(eventArgumentCaptor.capture(), anyString());
        assertEquals(BACKUP_ID, eventArgumentCaptor.getValue().getBackupRequest().getBackupId());
        assertEquals(BACKUP_LOCATION, eventArgumentCaptor.getValue().getBackupRequest().getBackupLocation());
        assertEquals(sdxCluster.getId(), eventArgumentCaptor.getValue().getResourceId());
        assertTrue(isUUID(eventArgumentCaptor.getValue().getDrStatus().getOperationId()));
    }

    @Test
    public void triggerDatabaseBackupInternalSuccess() {
        String drOperationId = UUID.randomUUID().toString();
        when(datalakeDrClient.triggerBackup(any(), any(), any(), any(), any()))
                .thenReturn(new DatalakeBackupStatusResponse(drOperationId, DatalakeOperationStatus.State.IN_PROGRESS, List.of(), "", null, ""));
        when(sdxClusterRepository.findById(sdxCluster.getId())).thenReturn(Optional.of(sdxCluster));
        DatalakeBackupStatusResponse backupResponse = underTest.triggerDatalakeBackup(sdxCluster.getId(), BACKUP_LOCATION, BACKUP_NAME, USER_CRN,
                new DatalakeDrSkipOptions(false, false, false, false));
        assertNotNull(backupResponse);
        assertEquals(drOperationId, backupResponse.getBackupId());
        assertTrue(isUUID(backupResponse.getBackupId()));
    }

    @Test
    public void testgetDatabaseBackupStatus() {
        when(sdxOperationRepository.findSdxOperationByOperationId(Mockito.anyString())).thenReturn(null);
        try {
            underTest.getDatabaseBackupStatus(sdxCluster, BACKUP_ID);
            fail("Exception should have been thrown");
        } catch (NotFoundException notFoundException) {
            String exceptedMessage = String.format("Status with id: [%s] not found", BACKUP_ID);
            assertEquals(exceptedMessage, notFoundException.getLocalizedMessage());
        }

        reset(sdxOperationRepository);
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setOperationType(SdxOperationType.RESTORE);
        sdxOperation.setSdxClusterId(sdxCluster.getId());
        sdxOperation.setOperationId(BACKUP_ID);
        when(sdxOperationRepository.findSdxOperationByOperationId(Mockito.anyString())).thenReturn(sdxOperation);
        try {
            underTest.getDatabaseBackupStatus(sdxCluster, BACKUP_ID);
            fail("Exception should have been thrown");
        } catch (CloudbreakApiException cloudbreakApiException) {
            String exceptedMessage = String.format("Invalid operation-id: [%s]. provided", BACKUP_ID);
            assertEquals(exceptedMessage, cloudbreakApiException.getLocalizedMessage());
        }
    }

    @Test
    public void triggerDatabaseRestoreSuccess() {
        when(sdxReactorFlowManager.triggerDatalakeDatabaseRestoreFlow(any(DatalakeDatabaseRestoreStartEvent.class), anyString())).thenReturn(FLOW_IDENTIFIER);
        SdxDatabaseRestoreResponse restoreResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.triggerDatabaseRestore(sdxCluster, BACKUP_ID, RESTORE_ID, BACKUP_LOCATION, 0));
        assertEquals(FLOW_IDENTIFIER, restoreResponse.getFlowIdentifier());
        ArgumentCaptor<DatalakeDatabaseRestoreStartEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DatalakeDatabaseRestoreStartEvent.class);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseRestoreFlow(eventArgumentCaptor.capture(), anyString());
        assertEquals(BACKUP_ID, eventArgumentCaptor.getValue().getBackupId());
        assertEquals(RESTORE_ID, eventArgumentCaptor.getValue().getRestoreId());
        assertEquals(BACKUP_LOCATION, eventArgumentCaptor.getValue().getBackupLocation());
        assertEquals(USER_CRN, eventArgumentCaptor.getValue().getUserId());
        assertEquals(sdxCluster.getId(), eventArgumentCaptor.getValue().getResourceId());
        assertTrue(isUUID(eventArgumentCaptor.getValue().getDrStatus().getOperationId()));
    }

    @Test
    public void triggerDatalakeRestoreWhenInvalidBackupIdProvided() {
        SdxCluster sdxCluster = getValidSdxCluster();
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(true, true, true, true);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.triggerDatalakeRestore(sdxCluster, sdxCluster.getName(), BACKUP_ID,
                "/.", skipOptions, 10, true));
        assertEquals(String.format("Backup Id %s does not exist for data lake: %s", BACKUP_ID, sdxCluster.getClusterName()), exception.getMessage());
    }

    @Test
    public void triggerDatalakeRestoreWithLatestBackup() {
        SdxCluster sdxCluster = getValidSdxCluster();
        DatalakeDrSkipOptions skipOptions = new DatalakeDrSkipOptions(true, true, true, true);
        datalakeDRProto.DatalakeBackupInfo backupInfo = datalakeDRProto.DatalakeBackupInfo.newBuilder().build();
        when(datalakeDrClient.getLastSuccessfulBackup(any(), any(), any())).thenReturn(backupInfo);
        underTest.triggerDatalakeRestore(sdxCluster, sdxCluster.getName(), null,
                "/.", skipOptions, 10, true);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeRestoreFlow(any(), any());
    }

    @Test
    public void testWhenSuccessfulBackupExistsThenItIsReturned() {
        datalakeDRProto.DatalakeBackupInfo datalakeBackupInfo = datalakeDRProto.DatalakeBackupInfo
                .newBuilder()
                .setRuntimeVersion(RUNTIME)
                .setOverallState("SUCCESSFUL")
                .build();

        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty())).thenReturn(datalakeBackupInfo);
        datalakeDRProto.DatalakeBackupInfo backupInfo = underTest.getLastSuccessfulBackupInfo(CLUSTER_NAME, USER_CRN);
        assertEquals(datalakeBackupInfo, backupInfo);
    }

    @Test
    public void whenGetLastSuccessfulBackupInfoWithRuntimeNotFound() {
        SdxCluster cluster = getValidSdxCluster();
        when(datalakeDrClient.getLastSuccessfulBackup(any(), any(), any())).thenReturn(null);
        Optional<datalakeDRProto.DatalakeBackupInfo> response = underTest.getLastSuccessfulBackupInfoWithRuntime(cluster.getName(), USER_CRN, RUNTIME);
        assertTrue(response.isEmpty());
    }

    @Test
    public void whenDatabaseBackupIsCalledItShouldCallBackupInternal() {
        SdxOperation operation = new SdxOperation();
        SdxCluster sdxCluster = getValidSdxCluster();
        BackupV4Response backupV4Response = new BackupV4Response();
        when(sdxClusterRepository.findById(sdxCluster.getId())).thenReturn(Optional.of(sdxCluster));
        SdxDatabaseBackupRequest databaseBackupRequest = new SdxDatabaseBackupRequest();
        when(stackV4Endpoint.backupDatabaseByNameInternal(0L, sdxCluster.getName(),
                databaseBackupRequest.getBackupId(),
                databaseBackupRequest.getBackupLocation(),
                databaseBackupRequest.isCloseConnections(), new ArrayList<>(), USER_CRN,
                databaseBackupRequest.getDatabaseMaxDurationInMin(),
                false)).thenReturn(backupV4Response);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.databaseBackup(operation, 1L, databaseBackupRequest));
        verify(cloudbreakFlowService, times(1)).saveLastCloudbreakFlowChainId(any(), any());
    }

    @Test
    public void whenDatabaseBackupIsCalledItCanThrowException() {
        SdxOperation operation = new SdxOperation();
        SdxCluster sdxCluster = getValidSdxCluster();
        BackupV4Response backupV4Response = new BackupV4Response();
        when(sdxClusterRepository.findById(sdxCluster.getId())).thenReturn(Optional.of(sdxCluster));
        SdxDatabaseBackupRequest databaseBackupRequest = new SdxDatabaseBackupRequest();
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Database backup failed");
        when(stackV4Endpoint.backupDatabaseByNameInternal(0L, sdxCluster.getName(),
                databaseBackupRequest.getBackupId(),
                databaseBackupRequest.getBackupLocation(),
                databaseBackupRequest.isCloseConnections(), new ArrayList<>(), USER_CRN,
                databaseBackupRequest.getDatabaseMaxDurationInMin(),
                false)).thenThrow(new WebApplicationException("Failed to get BackupInfo"));
        CloudbreakApiException exception = assertThrows(CloudbreakApiException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.databaseBackup(operation, 1L, databaseBackupRequest)));
        assertEquals(String.format("Database backup failed for datalake-id: [%s]. Message: [Database backup failed]",
                sdxCluster.getId()), exception.getMessage());
    }

    @Test
    public void whenDatabaseRestoreIsCalledItShouldCallBackupInternal() {
        SdxOperation operation = new SdxOperation();
        SdxCluster sdxCluster = getValidSdxCluster();
        RestoreV4Response restoreV4Response = new RestoreV4Response();
        String backupLocation = "/asdf";
        String backupId = "1";
        when(sdxClusterRepository.findById(sdxCluster.getId())).thenReturn(Optional.of(sdxCluster));
        SdxDatabaseBackupRequest databaseBackupRequest = new SdxDatabaseBackupRequest();
        when(stackV4Endpoint.restoreDatabaseByNameInternal(0L, sdxCluster.getName(),
                backupLocation,
                backupId,
                USER_CRN,
                0,
                false)).thenReturn(restoreV4Response);
        ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.databaseRestore(operation, 1L, "1", "/asdf", 0, false));
        verify(cloudbreakFlowService, times(1)).saveLastCloudbreakFlowChainId(any(), any());
    }

    @Test
    public void whenDatabaseRestoreIsCalledItCanThrowException() {
        SdxOperation operation = new SdxOperation();
        SdxCluster sdxCluster = getValidSdxCluster();
        String backupLocation = "/asdf";
        String backupId = "1";
        when(sdxClusterRepository.findById(sdxCluster.getId())).thenReturn(Optional.of(sdxCluster));
        SdxDatabaseBackupRequest databaseBackupRequest = new SdxDatabaseBackupRequest();
        when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn("Database backup failed");
        when(stackV4Endpoint.restoreDatabaseByNameInternal(0L, sdxCluster.getName(),
                backupLocation,
                backupId,
                USER_CRN,
                0,
                false)).thenThrow(new WebApplicationException("Failed to get BackupInfo"));
        CloudbreakApiException exception = assertThrows(CloudbreakApiException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                        () -> underTest.databaseRestore(operation, 1L, "1", "/asdf", 0, false)));
        assertEquals(String.format("Database restore failed for datalake-id: [%s]. Message: [Database backup failed]",
                sdxCluster.getId()), exception.getMessage());
    }

    @Test
    public void testShouldSdxBackupBePerformed() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster("7.2.14");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);

        assertTrue(underTest.shouldSdxBackupBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.0");
        assertTrue(!underTest.shouldSdxBackupBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.0");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.GCS);
        assertTrue(!underTest.shouldSdxBackupBePerformed(sdxCluster));


        sdxCluster = getValidSdxCluster("7.2.1");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS_GEN_2);
        assertTrue(!underTest.shouldSdxBackupBePerformed(sdxCluster));
    }

    @Test
    public void testShouldSdxRestoreBePerformed() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);

        SdxCluster sdxCluster = getValidSdxCluster("7.2.13");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        assertTrue(underTest.shouldSdxRestoreBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.14");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        assertTrue(underTest.shouldSdxRestoreBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.15");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        assertTrue(underTest.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AZURE");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS_GEN_2);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        assertTrue(underTest.shouldSdxRestoreBePerformed(sdxCluster));


        detailedEnvironmentResponse.setCloudPlatform("AWS");
        sdxCluster = getValidSdxCluster("7.2.13");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRangerRazEnabled(true);
        assertTrue(!underTest.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AWS");
        sdxCluster = getValidSdxCluster("7.2.14");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRangerRazEnabled(true);
        assertTrue(!underTest.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AWS");
        sdxCluster = getValidSdxCluster("7.2.15");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRangerRazEnabled(true);
        assertTrue(underTest.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AZURE");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS_GEN_2);
        sdxCluster.setRangerRazEnabled(true);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        assertTrue(underTest.shouldSdxRestoreBePerformed(sdxCluster));
    }

    @Test
    public void testWhenSuccessfulBackupDoesNotExistThenThrowError() {

        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty())).thenReturn(null);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.getLastSuccessfulBackupInfo(CLUSTER_NAME, USER_CRN));
        assertEquals("No successful backup found for data lake: " + CLUSTER_NAME, exception.getMessage());
    }

    @Test
    public void testCheckExistingBackup() {
        Date currentDate = new Date();
        currentDate.setHours(new Date().getHours() - 90);
        datalakeDRProto.DatalakeBackupInfo datalakeBackupInfo = datalakeDRProto.DatalakeBackupInfo
                .newBuilder()
                .setRuntimeVersion(RUNTIME)
                .setOverallState("SUCCESSFUL")
                .setEndTimestamp(String.valueOf(currentDate.getTime()))
                .build();
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty()))
                .thenReturn(datalakeBackupInfo);

        assertThrows(BadRequestException.class, () -> underTest.checkExistingBackup(sdxCluster, USER_CRN));
    }

    @Test
    public void testCheckExistingBackupThereIsBackupIn90Days() {
        Date currentDate = new Date();
        currentDate.setHours(new Date().getHours() - 10);
        datalakeDRProto.DatalakeBackupInfo datalakeBackupInfo = datalakeDRProto.DatalakeBackupInfo
                .newBuilder()
                .setRuntimeVersion(RUNTIME)
                .setOverallState("SUCCESSFUL")
                .setEndTimestamp(String.valueOf(currentDate.getTime()))
                .build();
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty()))
                .thenReturn(datalakeBackupInfo);
        SdxCluster sdxCluster = getValidSdxCluster();
        underTest.checkExistingBackup(sdxCluster, USER_CRN);
    }

    @Test
    public void testCheckExistingBackupThereIsNoBackupIn90Days() {
        Date currentDate = new Date();
        currentDate.setHours(new Date().getHours() - 90);
        datalakeDRProto.DatalakeBackupInfo datalakeBackupInfo = datalakeDRProto.DatalakeBackupInfo
                .newBuilder()
                .setRuntimeVersion(RUNTIME)
                .setOverallState("SUCCESSFUL")
                .setEndTimestamp(String.valueOf(currentDate.getTime()))
                .build();
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty()))
                .thenReturn(datalakeBackupInfo);
        assertThrows(BadRequestException.class, () -> underTest.checkExistingBackup(sdxCluster, USER_CRN));
    }

    @Test
    public void testCheckExistingBackupException() {
        Date currentDate = new Date();
        currentDate.setHours(new Date().getHours() - 10);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty()))
                .thenReturn(null);
        assertThrows(BadRequestException.class, () -> underTest.checkExistingBackup(sdxCluster, USER_CRN));
    }

    @Test
    public void testCreateDatabaseBackupRestoreErrorStage() {
        Long testId = 1L;
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxService.getById(testId)).thenReturn(sdxCluster);
        FlowCheckResponse testFlowCheckResponseWithAllInfo = new FlowCheckResponse();
        testFlowCheckResponseWithAllInfo.setCurrentState("test-current-state");
        testFlowCheckResponseWithAllInfo.setNextEvent("test-next-event");
        testFlowCheckResponseWithAllInfo.setFlowType("test1.test2.test-flow-type");
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseWithAllInfo);
        String errorStageWithAllInfo = underTest.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals(" during the transition from test-current-state to its next state, " +
                "set up in test-flow-type, triggered by test-next-event", errorStageWithAllInfo);

        FlowCheckResponse testFlowCheckResponseMissingCurrentState = new FlowCheckResponse();
        testFlowCheckResponseMissingCurrentState.setCurrentState(null);
        testFlowCheckResponseMissingCurrentState.setNextEvent("test-next-event");
        testFlowCheckResponseMissingCurrentState.setFlowType("test1.test2.test-flow-type");
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseMissingCurrentState);
        String errorStageMissingCurrentState = underTest.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals("", errorStageMissingCurrentState);

        FlowCheckResponse testFlowCheckResponseMissingNextEvent = new FlowCheckResponse();
        testFlowCheckResponseMissingNextEvent.setCurrentState("test-current-state");
        testFlowCheckResponseMissingNextEvent.setNextEvent(null);
        testFlowCheckResponseMissingNextEvent.setFlowType("test1.test2.test-flow-type");
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseMissingNextEvent);
        String errorStageMissingNextEvent = underTest.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals(" during the transition from test-current-state to its next state, " +
                "set up in test-flow-type", errorStageMissingNextEvent);

        FlowCheckResponse testFlowCheckResponseMissingFlowType = new FlowCheckResponse();
        testFlowCheckResponseMissingFlowType.setCurrentState("test-current-state");
        testFlowCheckResponseMissingFlowType.setNextEvent("test-next-event");
        testFlowCheckResponseMissingFlowType.setFlowType(null);
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseMissingFlowType);
        String errorStageMissingFlowType = underTest.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals(" during the transition from test-current-state to its next state, " +
                "triggered by test-next-event", errorStageMissingFlowType);
    }

    @Test
    public void testIsDatalakeInBackupOrRestoreWithoutExceptions() {
        DatalakeBackupStatusResponse backupStatusInProgress = new DatalakeBackupStatusResponse("",
                DatalakeOperationStatus.State.IN_PROGRESS, Collections.emptyList(), "", "", "");
        DatalakeBackupStatusResponse backupStatusCompleted = new DatalakeBackupStatusResponse("",
                DatalakeOperationStatus.State.SUCCESSFUL, Collections.emptyList(), "", "", "");
        DatalakeRestoreStatusResponse restoreStatusInProgress = new DatalakeRestoreStatusResponse("", "",
                DatalakeOperationStatus.State.STARTED, Collections.emptyList(), "");
        DatalakeRestoreStatusResponse restoreStatusCompleted = new DatalakeRestoreStatusResponse("", "",
                DatalakeOperationStatus.State.FAILED, Collections.emptyList(), "");

        // Missing actorCrn.
        SdxCluster sdxCluster = getValidSdxCluster();
        assertFalse(underTest.isDatalakeInBackupProgress(sdxCluster.getClusterName(), null));
        assertFalse(underTest.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), null));

        // Backup is in progress.
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenReturn(backupStatusInProgress);
        assertTrue(underTest.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));

        // Restore is in progress.
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenReturn(restoreStatusInProgress);
        assertTrue(underTest.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));

        // Backup is completed
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenReturn(backupStatusCompleted);
        assertFalse(underTest.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));

        // Restore is completed
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenReturn(restoreStatusCompleted);
        assertFalse(underTest.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInBackupWithNeverHaveBackup() {
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenThrow(new RuntimeException("Status information for backup operation on " +
                "datalake: sdxcluster not found"));
        assertFalse(underTest.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInRestoreWithNeverHaveRestore() {
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenThrow(new RuntimeException("Status information for restore operation on " +
                "datalake: sdxcluster not found"));
        assertFalse(underTest.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInBackupWithOtherRuntimeExceptions() {
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenThrow(new RuntimeException("Other run time exception"));
        assertFalse(underTest.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInRestoreWithOtherRuntimeExceptions() {
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenThrow(new RuntimeException("Other run time exception"));
        assertFalse(underTest.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testModifyBackupLocationWithAWS() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setRangerRazEnabled(true);
        assertEquals("s3://bucket/backups", underTest.modifyBackupLocation(sdxCluster, "s3://bucket"));
        assertEquals("s3://bucket/backups", underTest.modifyBackupLocation(sdxCluster, "s3://bucket/"));
        assertEquals("s3://bucket/test", underTest.modifyBackupLocation(sdxCluster, "s3://bucket/test"));
        assertEquals("s3://bucket/test/test1", underTest.modifyBackupLocation(sdxCluster, "s3://bucket/test/test1"));
        assertEquals("s3:/bucket", underTest.modifyBackupLocation(sdxCluster, "s3:/bucket"));
        assertEquals("s3://", underTest.modifyBackupLocation(sdxCluster, "s3://"));

        sdxCluster.setRangerRazEnabled(false);
        assertEquals("s3://bucket", underTest.modifyBackupLocation(sdxCluster, "s3://bucket"));
        assertEquals("s3://bucket/", underTest.modifyBackupLocation(sdxCluster, "s3://bucket/"));
        assertEquals("s3://bucket/test", underTest.modifyBackupLocation(sdxCluster, "s3://bucket/test"));
        assertEquals("s3://bucket/test/test1", underTest.modifyBackupLocation(sdxCluster, "s3://bucket/test/test1"));
    }

    @Test
    public void testModifyBackupLocationWithAzure() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AZURE");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setRangerRazEnabled(true);
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/backups",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/backups",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test/test1",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test/test1"));
        assertEquals("abfs:/test@mydatalake.dfs.core.windows.net",
                underTest.modifyBackupLocation(sdxCluster, "abfs:/test@mydatalake.dfs.core.windows.net"));
        assertEquals("abfs://",
                underTest.modifyBackupLocation(sdxCluster, "abfs://"));

        sdxCluster.setRangerRazEnabled(false);
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test/test1",
                underTest.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test/test1"));
    }

    @Test
    public void testModifyBackupLocationWithGCP() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("GCP");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setRangerRazEnabled(true);
        assertEquals("gs://bucket", underTest.modifyBackupLocation(sdxCluster, "gs://bucket"));
        assertEquals("gs://bucket/", underTest.modifyBackupLocation(sdxCluster, "gs://bucket/"));
        assertEquals("gs://bucket/test", underTest.modifyBackupLocation(sdxCluster, "gs://bucket/test"));

        sdxCluster.setRangerRazEnabled(false);
        assertEquals("gs://bucket", underTest.modifyBackupLocation(sdxCluster, "gs://bucket"));
        assertEquals("gs://bucket/", underTest.modifyBackupLocation(sdxCluster, "gs://bucket/"));
        assertEquals("gs://bucket/test", underTest.modifyBackupLocation(sdxCluster, "gs://bucket/test"));

    }

    @Test
    public void testGetTotalDurationWithValidTimestamps() {
        String startTimeStamp = "2024-10-02T19:09:31.000653+00:00";
        String endTimeStamp = "2024-10-02T19:15:12.000060+00:00";
        assertEquals(5, underTest.getTotalDurationInMin(startTimeStamp, endTimeStamp));
    }

    @Test
    public void testGetTotalDurationWithEmptyTimestamps() {
        String startTimeStamp = "2024-10-02T19:09:31.000653+00:00";
        String endTimeStamp = null;
        assertEquals(0, underTest.getTotalDurationInMin(startTimeStamp, endTimeStamp));
        endTimeStamp = "";
        assertEquals(0, underTest.getTotalDurationInMin(startTimeStamp, endTimeStamp));
    }

    @Test
    public void testGetTotalDurationWithInvalidTimestamps() {
        String startTimeStamp = "some tests";
        String endTimeStamp = "2024-10-02T19:09:31.000653+00:00";
        assertEquals(0, underTest.getTotalDurationInMin(startTimeStamp, endTimeStamp));
    }

    @Test
    public void getStackResponseAttemptResultWhenStackIsAvailable() {
        SdxCluster sdxCluster = getValidSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(Status.AVAILABLE);
        stackV4Response.setCluster(clusterV4Response);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getName()), any(), any())).thenReturn(stackV4Response);
        AttemptResult<StackV4Response> response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getStackResponseAttemptResult(sdxCluster, "Polling", FlowState.FINISHED));
        assertTrue(response.getState().equals(AttemptState.FINISH));
    }

    @Test
    public void getStackResponseAttemptResultWhenStackAndClusterFailure() {
        SdxCluster sdxCluster = getValidSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.UPDATE_FAILED);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(Status.UPDATE_FAILED);
        stackV4Response.setCluster(clusterV4Response);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getName()), any(), any())).thenReturn(stackV4Response);
        AttemptResult<StackV4Response> response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getStackResponseAttemptResult(sdxCluster, "Polling", FlowState.FINISHED));
        assertTrue(response.getState().equals(AttemptState.BREAK));
    }

    @Test
    public void getStackResponseAttemptResultWhenClusterFailure() {
        SdxCluster sdxCluster = getValidSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(Status.UPDATE_FAILED);
        stackV4Response.setCluster(clusterV4Response);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getName()), any(), any())).thenReturn(stackV4Response);
        AttemptResult<StackV4Response> response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getStackResponseAttemptResult(sdxCluster, "Polling", FlowState.FINISHED));
        assertTrue(response.getState().equals(AttemptState.BREAK));
    }

    @Test
    public void getStackResponseResultWhenBackupInProgressButStackAvailable() {
        SdxCluster sdxCluster = getValidSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(Status.BACKUP_IN_PROGRESS);
        stackV4Response.setCluster(clusterV4Response);
        when(stackV4Endpoint.get(eq(0L), eq(sdxCluster.getName()), any(), any())).thenReturn(stackV4Response);
        AttemptResult<StackV4Response> response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getStackResponseAttemptResult(sdxCluster, "Polling", FlowState.FINISHED));
        assertTrue(response.getState().equals(AttemptState.BREAK));
    }

    @Test
    public void getStackBackupRestoreErrorStage() {
        SdxCluster sdxCluster = getValidSdxCluster();
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(Status.BACKUP_IN_PROGRESS);
        stackV4Response.setCluster(clusterV4Response);
        when(sdxService.getById(any())).thenReturn(sdxCluster);
        FlowCheckResponse lastKnownFlowCheckResponse = new FlowCheckResponse();
        lastKnownFlowCheckResponse.setCurrentState("finished");
        lastKnownFlowCheckResponse.setFlowType("polling");
        lastKnownFlowCheckResponse.setNextEvent("nextevent");
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(lastKnownFlowCheckResponse);
        String response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.createDatabaseBackupRestoreErrorStage(sdxCluster.getId()));
        assertEquals(" during the transition from finished to its next state, set up in polling, triggered by nextevent", response);
    }

    @Test
    public void testSdxDatabaseBackupStatusConversion() {
        SdxCluster sdxCluster = getValidSdxCluster();
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setSdxClusterId(sdxCluster.getId());
        sdxOperation.setOperationType(SdxOperationType.BACKUP);
        sdxOperation.setStatus(SdxOperationStatus.SUCCEEDED);
        when(sdxOperationRepository.findSdxOperationByOperationId("0")).thenReturn(sdxOperation);
        SdxDatabaseBackupStatusResponse response = underTest.getDatabaseBackupStatus(sdxCluster, "0");
        assertEquals(DatalakeDatabaseDrStatus.SUCCEEDED, response.getStatus());
    }

    @Test
    public void testSdxDatabaseRestoreStatusConversion() {
        SdxCluster sdxCluster = getValidSdxCluster();
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setSdxClusterId(sdxCluster.getId());
        sdxOperation.setOperationType(SdxOperationType.RESTORE);
        sdxOperation.setStatus(SdxOperationStatus.SUCCEEDED);
        when(sdxOperationRepository.findSdxOperationByOperationId("0")).thenReturn(sdxOperation);
        SdxDatabaseRestoreStatusResponse response = underTest.getDatabaseRestoreStatus(sdxCluster, "0");
        assertEquals(DatalakeDatabaseDrStatus.SUCCEEDED, response.getStatus());
    }

    @Test
    public void testSdxDatabaseRestoreStatusConversionFromTriggeredToInProgress() {
        SdxCluster sdxCluster = getValidSdxCluster();
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setSdxClusterId(sdxCluster.getId());
        sdxOperation.setOperationType(SdxOperationType.RESTORE);
        sdxOperation.setStatus(SdxOperationStatus.TRIGGERRED);
        when(sdxOperationRepository.findSdxOperationByOperationId("0")).thenReturn(sdxOperation);
        SdxDatabaseRestoreStatusResponse response = underTest.getDatabaseRestoreStatus(sdxCluster, "0");
        assertEquals(DatalakeDatabaseDrStatus.TRIGGERRED, response.getStatus());
    }

    @Test
    public void testSdxDatabaseRestoreStatusConversionFromInProgressToInProgress() {
        SdxCluster sdxCluster = getValidSdxCluster();
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setSdxClusterId(sdxCluster.getId());
        sdxOperation.setOperationType(SdxOperationType.RESTORE);
        sdxOperation.setStatus(SdxOperationStatus.INPROGRESS);
        when(sdxOperationRepository.findSdxOperationByOperationId("0")).thenReturn(sdxOperation);
        SdxDatabaseRestoreStatusResponse response = underTest.getDatabaseRestoreStatus(sdxCluster, "0");
        assertEquals(DatalakeDatabaseDrStatus.INPROGRESS, response.getStatus());
    }

    @Test
    public void testSdxDatabaseRestoreWhenOperationNotFound() {
        SdxCluster sdxCluster = getValidSdxCluster();
        when(sdxOperationRepository.findSdxOperationByOperationId("0")).thenReturn(null);
        assertThrows(NotFoundException.class, () -> underTest.getDatabaseRestoreStatus(sdxCluster, "0"));
    }

    @Test
    public void testSdxDatabaseRestoreWhenOperationIdDoesNotMatch() {
        SdxCluster sdxCluster = getValidSdxCluster();
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setSdxClusterId(2L);
        sdxOperation.setOperationType(SdxOperationType.RESTORE);
        sdxOperation.setStatus(SdxOperationStatus.SUCCEEDED);
        when(sdxOperationRepository.findSdxOperationByOperationId("0")).thenReturn(sdxOperation);
        assertThrows(CloudbreakApiException.class, () -> underTest.getDatabaseRestoreStatus(sdxCluster, "0"));
    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setAccountId("accountId");
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setSdxDatabase(sdxDatabase);
        sdxCluster.setId(1L);
        return sdxCluster;
    }

    private SdxCluster getValidSdxCluster(String runtime) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setRuntime(runtime);
        return sdxCluster;
    }

    private static boolean isUUID(String string) {
        try {
            UUID.fromString(string);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
