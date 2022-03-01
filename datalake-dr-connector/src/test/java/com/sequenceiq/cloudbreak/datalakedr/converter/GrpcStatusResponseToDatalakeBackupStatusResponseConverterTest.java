package com.sequenceiq.cloudbreak.datalakedr.converter;

import static com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter.FAILED_STATE;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Before;
import org.junit.Test;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;

public class GrpcStatusResponseToDatalakeBackupStatusResponseConverterTest {

    private static final String FAILURE_REASON = "Failed operation";

    private static final String FAILURE_REASON2 = "A different failure message.";

    private GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter();
    }

    @Test
    public void testSuccessfulBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("SUCCESSFUL");

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
    public void testFailedBackupAdminOperations() {
        datalakeDRProto.AdminOperationsBackupRestoreState.Builder adminBuilder =
            datalakeDRProto.AdminOperationsBackupRestoreState.newBuilder()
                .setStopServices(createStatus(FAILED_STATE, FAILURE_REASON))
                .setStartServices(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setAdminOperations(adminBuilder);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.STOP_SERVICES.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.START_SERVICES.description(), FAILURE_REASON2);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedHbaseBackupSameFailureReason() {
        datalakeDRProto.HbaseBackupRestoreState.Builder hbaseBuilder =
            datalakeDRProto.HbaseBackupRestoreState.newBuilder()
                .setAtlasJanusTable(createStatus(FAILED_STATE, FAILURE_REASON))
                .setAtlasEntityAuditEventTable(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setHbase(hbaseBuilder);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure = getFailureString(OperationEnum.HBASE.description(), FAILURE_REASON);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedHbaseBackupDifferentFailureReasons() {
        datalakeDRProto.HbaseBackupRestoreState.Builder hbaseBuilder =
            datalakeDRProto.HbaseBackupRestoreState.newBuilder()
                .setAtlasJanusTable(createStatus(FAILED_STATE, FAILURE_REASON))
                .setAtlasEntityAuditEventTable(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setHbase(hbaseBuilder);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.HBASE_ATLAS_JANUS.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.HBASE_ATLAS_AUDIT.description(), FAILURE_REASON2);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedSolrBackupSameFailureReason() {
        datalakeDRProto.SolrBackupRestoreState.Builder solrBuilder =
            datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setEdgeIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setFulltextIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setRangerAuditsCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setVertexIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solrBuilder);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure = getFailureString(OperationEnum.SOLR.description(), FAILURE_REASON);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedSolrBackupDifferentFailureReasons() {
        datalakeDRProto.SolrBackupRestoreState.Builder solrBuilder =
            datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setEdgeIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setFulltextIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solrBuilder);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.SOLR_EDGE_INDEX.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_FULLTEXT_INDEX.description(), FAILURE_REASON2);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedDatabaseBackup() {
        datalakeDRProto.DatabaseBackupRestoreState.Builder databaseBuilder =
            datalakeDRProto.DatabaseBackupRestoreState.newBuilder()
                .setDatabase(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setDatabase(databaseBuilder);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure = getFailureString(OperationEnum.DATABASE.description(), FAILURE_REASON);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInvalidBackupStatus() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
            datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState("INVALID_STATUS");

        assertThrows(IllegalArgumentException.class, () -> {
            underTest.convert(builder.build());
        });
    }

    @Test
    public void testSuccessfulRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("SUCCESSFUL");

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
    public void testFailedRestoreAdminOperations() {
        datalakeDRProto.AdminOperationsBackupRestoreState.Builder adminBuilder =
            datalakeDRProto.AdminOperationsBackupRestoreState.newBuilder()
                .setStopServices(createStatus(FAILED_STATE, FAILURE_REASON))
                .setStartServices(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setAdminOperations(adminBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.STOP_SERVICES.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.START_SERVICES.description(), FAILURE_REASON2);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedHbaseRestoreSameFailureReason() {
        datalakeDRProto.HbaseBackupRestoreState.Builder hbaseBuilder =
            datalakeDRProto.HbaseBackupRestoreState.newBuilder()
                .setAtlasJanusTable(createStatus(FAILED_STATE, FAILURE_REASON))
                .setAtlasEntityAuditEventTable(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setHbase(hbaseBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure = getFailureString(OperationEnum.HBASE.description(), FAILURE_REASON);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedHbaseRestoreDifferentFailureReasons() {
        datalakeDRProto.HbaseBackupRestoreState.Builder hbaseBuilder =
            datalakeDRProto.HbaseBackupRestoreState.newBuilder()
                .setAtlasJanusTable(createStatus(FAILED_STATE, FAILURE_REASON))
                .setAtlasEntityAuditEventTable(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setHbase(hbaseBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.HBASE_ATLAS_JANUS.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.HBASE_ATLAS_AUDIT.description(), FAILURE_REASON2);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedSolrRestoreSameFailureReason() {
        datalakeDRProto.SolrBackupRestoreState.Builder solrBuilder =
            datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setEdgeIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setFulltextIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setRangerAuditsCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setVertexIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solrBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure = getFailureString(OperationEnum.SOLR.description(), FAILURE_REASON);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedSolrRestoreDifferentFailureReasons() {
        datalakeDRProto.SolrBackupRestoreState.Builder solrBuilder =
            datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setEdgeIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setFulltextIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setRangerAuditsCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setVertexIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solrBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.SOLR_EDGE_INDEX.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_FULLTEXT_INDEX.description(), FAILURE_REASON2) + ", " +
                getFailureString(OperationEnum.SOLR_RANGER_AUDITS.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_VERTEX_INDEX.description(), FAILURE_REASON2);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedSolrRestoreSameFailureReasonWithDelete() {
        datalakeDRProto.SolrBackupRestoreState.Builder solrBuilder =
            datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setEdgeIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setFulltextIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setRangerAuditsCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setVertexIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setEdgeIndexCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setFulltextIndexCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setRangerAuditsCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setVertexIndexCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON2));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solrBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.SOLR.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_DELETE.description(), FAILURE_REASON2);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedSolrRestoreSameFailureReasonWithDeleteDifferentReasons() {
        datalakeDRProto.SolrBackupRestoreState.Builder solrBuilder =
            datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setEdgeIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setFulltextIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setRangerAuditsCollection(createStatus(FAILED_STATE, FAILURE_REASON))
                .setVertexIndexCollection(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setEdgeIndexCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setFulltextIndexCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON))
                .setRangerAuditsCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON2))
                .setVertexIndexCollectionDelete(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
            datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solrBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState(FAILED_STATE)
                .setOperationStates(stateBuilder);

        String expectedFailure =
                getFailureString(OperationEnum.SOLR_EDGE_INDEX.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_FULLTEXT_INDEX.description(), FAILURE_REASON2) + ", " +
                getFailureString(OperationEnum.SOLR_RANGER_AUDITS.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_VERTEX_INDEX.description(), FAILURE_REASON2) + ", " +
                getFailureString(OperationEnum.SOLR_EDGE_INDEX_DELETE.description(), FAILURE_REASON2) + ", " +
                getFailureString(OperationEnum.SOLR_FULLTEXT_INDEX_DELETE.description(), FAILURE_REASON) + ", " +
                getFailureString(OperationEnum.SOLR_RANGER_AUDITS_DELETE.description(), FAILURE_REASON2) + ", " +
                getFailureString(OperationEnum.SOLR_VERTEX_INDEX_DELETE.description(), FAILURE_REASON);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testFailedDatabaseRestore() {
        datalakeDRProto.DatabaseBackupRestoreState.Builder databaseBuilder =
                datalakeDRProto.DatabaseBackupRestoreState.newBuilder()
                        .setDatabase(createStatus(FAILED_STATE, FAILURE_REASON));
        datalakeDRProto.InternalBackupRestoreState.Builder stateBuilder =
                datalakeDRProto.InternalBackupRestoreState.newBuilder()
                        .setDatabase(databaseBuilder);
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                        .setOverallState(FAILED_STATE)
                        .setOperationStates(stateBuilder);

        String expectedFailure = getFailureString(OperationEnum.DATABASE.description(), FAILURE_REASON);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testInvalidRestoreStatus() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("INVALID_STATUS");

        assertThrows(IllegalArgumentException.class, () -> {
            underTest.convert(builder.build());
        });
    }

    @Test
    public void testBackupLegacyFailureReason() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
                datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                        .setOverallState(FAILED_STATE)
                        .setFailureReason(FAILURE_REASON);

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeBackupStatusResponse.State.FAILED, response.getState());
        assertEquals(FAILURE_REASON, response.getFailureReason());
        assert response.isComplete();
    }

    @Test
    public void testRestoreLegacyFailureReason() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                        .setOverallState(FAILED_STATE)
                        .setFailureReason(FAILURE_REASON);

        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(FAILURE_REASON, response.getFailureReason());
        assert response.isComplete();
    }

    private datalakeDRProto.BackupRestoreOperationStatus.Builder createStatus(String status, String failureReason) {
        datalakeDRProto.BackupRestoreOperationStatus.Builder statusBuilder =
            datalakeDRProto.BackupRestoreOperationStatus.newBuilder();
        if (status != null) {
            statusBuilder.setStatus(status);
        }
        if (failureReason != null) {
            statusBuilder.setFailureReason(failureReason);
        }
        return statusBuilder;
    }

    private String getFailureString(String operationName, String failureMessage) {
        return operationName + ": " + failureMessage;
    }
}
