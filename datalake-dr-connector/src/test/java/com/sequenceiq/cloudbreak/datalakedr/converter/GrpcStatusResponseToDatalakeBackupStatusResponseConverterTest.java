package com.sequenceiq.cloudbreak.datalakedr.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Before;
import org.junit.Test;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;

public class GrpcStatusResponseToDatalakeBackupStatusResponseConverterTest {

    private static final String FAILURE_REASON = "Failed operation";

    private GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter();
    }

    @Test
    public void testSuccessfulBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("SUCCESSFUL")
                .setFailureReason("null");

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.SUCCESSFUL, response.getState());
        assertEquals(DatalakeBackupStatusResponse.NO_FAILURES, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInProgressBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("IN_PROGRESS");

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.IN_PROGRESS, response.getState());
        assert !response.isComplete();
    }

    @Test
    public void testFailedBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("FAILED")
                .setFailureReason(FAILURE_REASON);

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(FAILURE_REASON, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInvalidBackupStatus() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("INVALID_STAUS");

        assertThrows(IllegalArgumentException.class, () -> {
            underTest.convert(builder.build());
        });
    }

    @Test
    public void testSuccessfulRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("SUCCESSFUL")
                .setFailureReason("null");

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.SUCCESSFUL, response.getState());
        assertEquals(DatalakeBackupStatusResponse.NO_FAILURES, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInProgressRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("IN_PROGRESS");

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.IN_PROGRESS, response.getState());
        assert !response.isComplete();
    }

    @Test
    public void testFailedRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("FAILED")
                .setFailureReason(FAILURE_REASON);

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(FAILURE_REASON, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInvalidRestoreStatus() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("INVALID_STAUS");

        assertThrows(IllegalArgumentException.class, () -> {
            underTest.convert(builder.build());
        });
    }
}
