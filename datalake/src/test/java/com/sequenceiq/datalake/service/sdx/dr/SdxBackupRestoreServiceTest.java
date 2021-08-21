package com.sequenceiq.datalake.service.sdx.dr;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.assertj.core.util.Strings;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.operation.SdxOperation;
import com.sequenceiq.datalake.entity.operation.SdxOperationType;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeDatabaseBackupStartEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeDatabaseRestoreStartEvent;
import com.sequenceiq.datalake.repository.SdxOperationRepository;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseBackupResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRestoreResponse;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class SdxBackupRestoreServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String BACKUPID = UUID.randomUUID().toString();

    private static final String RESTOREID = UUID.randomUUID().toString();

    private static final String BACKUPLOCATION = "location/of/backup";

    private static final String DBHOST = "loclhost";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String POLLABLE_ID = "datalake-dr-flow";

    private static FlowIdentifier flowIdentifier;

    private  static String userCrn;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxOperationRepository sdxOperationRepository;

    @InjectMocks
    private SdxBackupRestoreService sdxBackupRestoreService;

    private SdxCluster sdxCluster;

    @BeforeClass
    public static void setup() {
        userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (Strings.isNullOrEmpty(userCrn)) {
            ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
            userCrn = USER_CRN;
        }
        flowIdentifier = new FlowIdentifier(FlowType.FLOW, POLLABLE_ID);
    }

    @Before
    public void initialize() {
        sdxCluster = getValidSdxCluster();
    }

    @Test
    public void triggerDatabaseBackupSuccess() {
        when(sdxReactorFlowManager.triggerDatalakeDatabaseBackupFlow(Mockito.any(DatalakeDatabaseBackupStartEvent.class))).thenReturn(flowIdentifier);
        SdxDatabaseBackupRequest backupRequest = new SdxDatabaseBackupRequest();
        backupRequest.setBackupId(BACKUPID);
        backupRequest.setBackupLocation(BACKUPLOCATION);
        backupRequest.setCloseConnections(true);
        SdxDatabaseBackupResponse backupResponse = sdxBackupRestoreService.triggerDatabaseBackup(sdxCluster, backupRequest);
        Assert.assertEquals(flowIdentifier, backupResponse.getFlowIdentifier());
        ArgumentCaptor<DatalakeDatabaseBackupStartEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DatalakeDatabaseBackupStartEvent.class);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseBackupFlow(eventArgumentCaptor.capture());
        Assert.assertEquals(BACKUPID, eventArgumentCaptor.getValue().getBackupRequest().getBackupId());
        Assert.assertEquals(BACKUPLOCATION, eventArgumentCaptor.getValue().getBackupRequest().getBackupLocation());
        Assert.assertEquals(userCrn, eventArgumentCaptor.getValue().getUserId());
        Assert.assertEquals(sdxCluster.getId(), eventArgumentCaptor.getValue().getResourceId());
        Assert.assertTrue(isUUID(eventArgumentCaptor.getValue().getDrStatus().getOperationId()));
    }

    @Test
    public void testgetDatabaseBackupStatus() {
        when(sdxOperationRepository.findSdxOperationByOperationId(Mockito.anyString())).thenReturn(null);
        try {
            sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, BACKUPID);
            Assert.fail("Exception should have been thrown");
        } catch (NotFoundException notFoundException) {
            String exceptedMessage = String.format("Status with id: [%s] not found", BACKUPID);
            Assert.assertEquals(exceptedMessage, notFoundException.getLocalizedMessage());
        }

        reset(sdxOperationRepository);
        SdxOperation sdxOperation = new SdxOperation();
        sdxOperation.setOperationType(SdxOperationType.RESTORE);
        sdxOperation.setSdxClusterId(sdxCluster.getId());
        sdxOperation.setOperationId(BACKUPID);
        when(sdxOperationRepository.findSdxOperationByOperationId(Mockito.anyString())).thenReturn(sdxOperation);
        try {
            sdxBackupRestoreService.getDatabaseBackupStatus(sdxCluster, BACKUPID);
            Assert.fail("Exception should have been thrown");
        } catch (CloudbreakApiException cloudbreakApiException) {
            String exceptedMessage = String.format("Invalid operation-id: [%s]. provided", BACKUPID);
            Assert.assertEquals(exceptedMessage, cloudbreakApiException.getLocalizedMessage());
        }
    }

    @Test
    public void triggerDatabaseRestoreSuccess() {
        when(sdxReactorFlowManager.triggerDatalakeDatabaseRestoreFlow(Mockito.any(DatalakeDatabaseRestoreStartEvent.class))).thenReturn(flowIdentifier);
        SdxDatabaseRestoreResponse restoreResponse = sdxBackupRestoreService.triggerDatabaseRestore(sdxCluster, BACKUPID, RESTOREID, BACKUPLOCATION);
        Assert.assertEquals(flowIdentifier, restoreResponse.getFlowIdentifier());
        ArgumentCaptor<DatalakeDatabaseRestoreStartEvent> eventArgumentCaptor = ArgumentCaptor.forClass(DatalakeDatabaseRestoreStartEvent.class);
        verify(sdxReactorFlowManager, times(1)).triggerDatalakeDatabaseRestoreFlow(eventArgumentCaptor.capture());
        Assert.assertEquals(BACKUPID, eventArgumentCaptor.getValue().getBackupId());
        Assert.assertEquals(RESTOREID, eventArgumentCaptor.getValue().getRestoreId());
        Assert.assertEquals(BACKUPLOCATION, eventArgumentCaptor.getValue().getBackupLocation());
        Assert.assertEquals(userCrn, eventArgumentCaptor.getValue().getUserId());
        Assert.assertEquals(sdxCluster.getId(), eventArgumentCaptor.getValue().getResourceId());
        Assert.assertTrue(isUUID(eventArgumentCaptor.getValue().getDrStatus().getOperationId()));
    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setId(1L);
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
