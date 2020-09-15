package com.sequenceiq.cloudbreak.datalakedr.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;

import org.junit.Before;
import org.junit.Test;

public class GrpcStatusResponseToDatalakeDrStatusResponseConverterTest {

    private static final String FAILURE_REASON = "Failed operation";

    private GrpcStatusResponseToDatalakeDrStatusResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new GrpcStatusResponseToDatalakeDrStatusResponseConverter();
    }

    @Test
    public void testSuccessfulBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("SUCCESSFUL")
                .setFailureReason("null");

        DatalakeDrStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeDrStatusResponse.State.SUCCESSFUL, response.getState());
        assertEquals(DatalakeDrStatusResponse.NO_FAILURES, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInProgressBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("IN_PROGRESS");

        DatalakeDrStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeDrStatusResponse.State.IN_PROGRESS, response.getState());
        assert !response.isComplete();
    }

    @Test
    public void testFailedBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("FAILED")
                .setFailureReason(FAILURE_REASON);

        DatalakeDrStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeDrStatusResponse.State.FAILED, response.getState());
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

        DatalakeDrStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeDrStatusResponse.State.SUCCESSFUL, response.getState());
        assertEquals(DatalakeDrStatusResponse.NO_FAILURES, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInProgressRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("IN_PROGRESS");

        DatalakeDrStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeDrStatusResponse.State.IN_PROGRESS, response.getState());
        assert !response.isComplete();
    }

    @Test
    public void testFailedRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("FAILED")
                .setFailureReason(FAILURE_REASON);

        DatalakeDrStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeDrStatusResponse.State.FAILED, response.getState());
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
