package com.sequenceiq.datalake.service.sdx.dr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrClient;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.datalakedr.config.DatalakeDrConfig;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;

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

    @InjectMocks
    private SdxBackupRestoreService sdxBackupRestoreService;

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
        SdxDatabaseBackupResponse backupResponse = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> sdxBackupRestoreService.triggerDatabaseBackup(sdxCluster, backupRequest));
        assertEquals(FLOW_IDENTIFIER, backupResponse.getFlowIdentifier());
        ArgumentCaptor<DatalakeDatabaseBackupStartEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DatalakeDatabaseBackupStartEvent.class);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseBackupFlow(eventArgumentCaptor.capture(), anyString());
        assertEquals(BACKUP_ID, eventArgumentCaptor.getValue().getBackupRequest().getBackupId());
        assertEquals(BACKUP_LOCATION, eventArgumentCaptor.getValue().getBackupRequest().getBackupLocation());
        assertEquals(USER_CRN, eventArgumentCaptor.getValue().getUserId());
        assertEquals(sdxCluster.getId(), eventArgumentCaptor.getValue().getResourceId());
        assertTrue(isUUID(eventArgumentCaptor.getValue().getDrStatus().getOperationId()));
    }

    @Test
    public void triggerDatabaseBackupInternalSuccess() {
        String drOperationId = UUID.randomUUID().toString();
        when(datalakeDrClient.triggerBackup(any(), any(), any(), any(), any()))
                .thenReturn(new DatalakeBackupStatusResponse(drOperationId, DatalakeOperationStatus.State.IN_PROGRESS, List.of(), "", null));
        when(sdxClusterRepository.findById(sdxCluster.getId())).thenReturn(Optional.of(sdxCluster));
        DatalakeBackupStatusResponse backupResponse = sdxBackupRestoreService.triggerDatalakeBackup(sdxCluster.getId(), BACKUP_LOCATION, BACKUP_NAME, USER_CRN,
                new DatalakeDrSkipOptions(false, false, false, false));
        assertNotNull(backupResponse);
        assertEquals(drOperationId, backupResponse.getBackupId());
        assertTrue(isUUID(backupResponse.getBackupId()));
    }

    @Test
    public void testgetDatabaseBackupStatus() {
        when(sdxOperationRepository.findSdxOperationByOperationId(Mockito.anyString())).thenReturn(null);
        try {
            sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, BACKUP_ID);
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
            sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, BACKUP_ID);
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
                () -> sdxBackupRestoreService.triggerDatabaseRestore(sdxCluster, BACKUP_ID, RESTORE_ID, BACKUP_LOCATION, 0));
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
    public void testWhenSuccessfulBackupExistsThenItIsReturned() {
        datalakeDRProto.DatalakeBackupInfo datalakeBackupInfo = datalakeDRProto.DatalakeBackupInfo
                .newBuilder()
                .setRuntimeVersion(RUNTIME)
                .setOverallState("SUCCESSFUL")
                .build();

        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty())).thenReturn(datalakeBackupInfo);
        datalakeDRProto.DatalakeBackupInfo backupInfo = sdxBackupRestoreService.getLastSuccessfulBackupInfo(CLUSTER_NAME, USER_CRN);
        assertEquals(datalakeBackupInfo, backupInfo);
    }

    @Test
    public void testShouldSdxBackupBePerformed() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster("7.2.14");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);

        assertTrue(sdxBackupRestoreService.shouldSdxBackupBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.0");
        assertTrue(!sdxBackupRestoreService.shouldSdxBackupBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.0");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.GCS);
        assertTrue(!sdxBackupRestoreService.shouldSdxBackupBePerformed(sdxCluster));


        sdxCluster = getValidSdxCluster("7.2.1");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS_GEN_2);
        assertTrue(!sdxBackupRestoreService.shouldSdxBackupBePerformed(sdxCluster));
    }

    @Test
    public void testShouldSdxRestoreBePerformed() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);

        SdxCluster sdxCluster = getValidSdxCluster("7.2.13");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        assertTrue(sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.14");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        assertTrue(sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));

        sdxCluster = getValidSdxCluster("7.2.15");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        assertTrue(sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AZURE");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS_GEN_2);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        assertTrue(sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));


        detailedEnvironmentResponse.setCloudPlatform("AWS");
        sdxCluster = getValidSdxCluster("7.2.13");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRangerRazEnabled(true);
        assertTrue(!sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AWS");
        sdxCluster = getValidSdxCluster("7.2.14");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRangerRazEnabled(true);
        assertTrue(!sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AWS");
        sdxCluster = getValidSdxCluster("7.2.15");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.S3);
        sdxCluster.setRangerRazEnabled(true);
        assertTrue(sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));

        detailedEnvironmentResponse.setCloudPlatform("AZURE");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS_GEN_2);
        sdxCluster.setRangerRazEnabled(true);
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        assertTrue(sdxBackupRestoreService.shouldSdxRestoreBePerformed(sdxCluster));
    }

    @Test
    public void testWhenSuccessfulBackupDoesNotExistThenThrowError() {

        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty())).thenReturn(null);
        NotFoundException exception = assertThrows(NotFoundException.class, () -> sdxBackupRestoreService.getLastSuccessfulBackupInfo(CLUSTER_NAME, USER_CRN));
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

        assertThrows(BadRequestException.class, () -> sdxBackupRestoreService.checkExistingBackup(sdxCluster, USER_CRN));
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
        sdxBackupRestoreService.checkExistingBackup(sdxCluster, USER_CRN);
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
        assertThrows(BadRequestException.class, () -> sdxBackupRestoreService.checkExistingBackup(sdxCluster, USER_CRN));
    }

    @Test
    public void testCheckExistingBackupException() {
        Date currentDate = new Date();
        currentDate.setHours(new Date().getHours() - 10);
        when(datalakeDrConfig.isConfigured()).thenReturn(true);
        when(datalakeDrClient.getLastSuccessfulBackup(CLUSTER_NAME, USER_CRN, Optional.empty()))
                .thenReturn(null);
        assertThrows(BadRequestException.class, () -> sdxBackupRestoreService.checkExistingBackup(sdxCluster, USER_CRN));
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
        String errorStageWithAllInfo = sdxBackupRestoreService.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals(" during the transition from test-current-state to its next state, " +
                "set up in test-flow-type, triggered by test-next-event", errorStageWithAllInfo);

        FlowCheckResponse testFlowCheckResponseMissingCurrentState = new FlowCheckResponse();
        testFlowCheckResponseMissingCurrentState.setCurrentState(null);
        testFlowCheckResponseMissingCurrentState.setNextEvent("test-next-event");
        testFlowCheckResponseMissingCurrentState.setFlowType("test1.test2.test-flow-type");
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseMissingCurrentState);
        String errorStageMissingCurrentState = sdxBackupRestoreService.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals("", errorStageMissingCurrentState);

        FlowCheckResponse testFlowCheckResponseMissingNextEvent = new FlowCheckResponse();
        testFlowCheckResponseMissingNextEvent.setCurrentState("test-current-state");
        testFlowCheckResponseMissingNextEvent.setNextEvent(null);
        testFlowCheckResponseMissingNextEvent.setFlowType("test1.test2.test-flow-type");
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseMissingNextEvent);
        String errorStageMissingNextEvent = sdxBackupRestoreService.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals(" during the transition from test-current-state to its next state, " +
                "set up in test-flow-type", errorStageMissingNextEvent);

        FlowCheckResponse testFlowCheckResponseMissingFlowType = new FlowCheckResponse();
        testFlowCheckResponseMissingFlowType.setCurrentState("test-current-state");
        testFlowCheckResponseMissingFlowType.setNextEvent("test-next-event");
        testFlowCheckResponseMissingFlowType.setFlowType(null);
        when(cloudbreakFlowService.getLastKnownFlowCheckResponse(sdxCluster)).thenReturn(testFlowCheckResponseMissingFlowType);
        String errorStageMissingFlowType = sdxBackupRestoreService.createDatabaseBackupRestoreErrorStage(testId);
        assertEquals(" during the transition from test-current-state to its next state, " +
                "triggered by test-next-event", errorStageMissingFlowType);
    }

    @Test
    public void testIsDatalakeInBackupOrRestoreWithoutExceptions() {
        DatalakeBackupStatusResponse backupStatusInProgress = new DatalakeBackupStatusResponse("",
                DatalakeOperationStatus.State.IN_PROGRESS, Collections.emptyList(), "", "");
        DatalakeBackupStatusResponse backupStatusCompleted = new DatalakeBackupStatusResponse("",
                DatalakeOperationStatus.State.SUCCESSFUL, Collections.emptyList(), "", "");
        DatalakeRestoreStatusResponse restoreStatusInProgress = new DatalakeRestoreStatusResponse("", "",
                DatalakeOperationStatus.State.STARTED, "");
        DatalakeRestoreStatusResponse restoreStatusCompleted = new DatalakeRestoreStatusResponse("", "",
                DatalakeOperationStatus.State.FAILED, "");

        // Missing actorCrn.
        SdxCluster sdxCluster = getValidSdxCluster();
        assertFalse(sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(), null));
        assertFalse(sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), null));

        // Backup is in progress.
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenReturn(backupStatusInProgress);
        assertTrue(sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));

        // Restore is in progress.
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenReturn(restoreStatusInProgress);
        assertTrue(sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));

        // Backup is completed
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenReturn(backupStatusCompleted);
        assertFalse(sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));

        // Restore is completed
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenReturn(restoreStatusCompleted);
        assertFalse(sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInBackupWithNeverHaveBackup() {
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenThrow(new RuntimeException("Status information for backup operation on " +
                "datalake: sdxcluster not found"));
        assertFalse(sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInRestoreWithNeverHaveRestore() {
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenThrow(new RuntimeException("Status information for restore operation on " +
                "datalake: sdxcluster not found"));
        assertFalse(sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInBackupWithOtherRuntimeExceptions() {
        when(datalakeDrClient.getBackupStatus(anyString(), anyString())).thenThrow(new RuntimeException("Other run time exception"));
        assertFalse(sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testIsDatalakeInRestoreWithOtherRuntimeExceptions() {
        when(datalakeDrClient.getRestoreStatus(anyString(), anyString())).thenThrow(new RuntimeException("Other run time exception"));
        assertFalse(sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), USER_CRN));
    }

    @Test
    public void testModifyBackupLocationWithAWS() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setRangerRazEnabled(true);
        assertEquals("s3://bucket/backups", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket"));
        assertEquals("s3://bucket/backups", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket/"));
        assertEquals("s3://bucket/test", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket/test"));
        assertEquals("s3://bucket/test/test1", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket/test/test1"));
        assertEquals("s3:/bucket", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3:/bucket"));
        assertEquals("s3://", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://"));

        sdxCluster.setRangerRazEnabled(false);
        assertEquals("s3://bucket", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket"));
        assertEquals("s3://bucket/", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket/"));
        assertEquals("s3://bucket/test", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket/test"));
        assertEquals("s3://bucket/test/test1", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "s3://bucket/test/test1"));
    }

    @Test
    public void testModifyBackupLocationWithAzure() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("AZURE");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setRangerRazEnabled(true);
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/backups",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/backups",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test/test1",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test/test1"));
        assertEquals("abfs:/test@mydatalake.dfs.core.windows.net",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs:/test@mydatalake.dfs.core.windows.net"));
        assertEquals("abfs://",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://"));

        sdxCluster.setRangerRazEnabled(false);
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test"));
        assertEquals("abfs://test@mydatalake.dfs.core.windows.net/test/test1",
                sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "abfs://test@mydatalake.dfs.core.windows.net/test/test1"));
    }

    @Test
    public void testModifyBackupLocationWithGCP() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setCloudPlatform("GCP");
        when(environmentClientService.getByName(anyString())).thenReturn(detailedEnvironmentResponse);
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setRangerRazEnabled(true);
        assertEquals("gs://bucket", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "gs://bucket"));
        assertEquals("gs://bucket/", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "gs://bucket/"));
        assertEquals("gs://bucket/test", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "gs://bucket/test"));

        sdxCluster.setRangerRazEnabled(false);
        assertEquals("gs://bucket", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "gs://bucket"));
        assertEquals("gs://bucket/", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "gs://bucket/"));
        assertEquals("gs://bucket/test", sdxBackupRestoreService.modifyBackupLocation(sdxCluster, "gs://bucket/test"));

    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setDatabaseCrn("crn:sdxcluster");
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
