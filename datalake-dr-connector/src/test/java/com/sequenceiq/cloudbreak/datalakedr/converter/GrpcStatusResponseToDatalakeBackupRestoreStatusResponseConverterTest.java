package com.sequenceiq.cloudbreak.datalakedr.converter;

import static com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter.FAILED_STATE;
import static com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter.SUCCESSFUL_STATE;
import static com.sequenceiq.cloudbreak.datalakedr.converter.GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter.VALIDATION_FAILED_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;

class GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverterTest {

    private static final String FAILURE_REASON = "Failed operation";

    private static final String FAILURE_REASON2 = "A different failure message.";

    private static final String PRECHECKS_FAILED_REASON = "Precheck Failed";

    private GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter underTest = new GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter();

    @Test
    void testSuccessfulBackup() {
        datalakeDRProto.BackupRestoreOperationStatus.Builder status = datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus("SUCCESSFUL");
        datalakeDRProto.SolrBackupRestoreState.Builder solr = datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setFulltextIndexCollection(status)
                .setVertexIndexCollection(status)
                .setRangerAuditsCollection(status)
                .setEdgeIndexCollection(status);

        datalakeDRProto.DatabaseBackupRestoreState.Builder database = datalakeDRProto.DatabaseBackupRestoreState.newBuilder().setDatabase(status);
        datalakeDRProto.HbaseBackupRestoreState.Builder hbase = datalakeDRProto.HbaseBackupRestoreState.newBuilder()
                .setAtlasJanusTable(datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus(SUCCESSFUL_STATE).build())
                .setAtlasEntityAuditEventTable(datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus(SUCCESSFUL_STATE).build());


        datalakeDRProto.InternalBackupRestoreState.Builder backupState = datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setDatabase(database)
                .setHbase(hbase)
                .setSolr(solr);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder = datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(SUCCESSFUL_STATE)
                .setOperationStates(backupState.build());

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.SUCCESSFUL, response.getState());
        assertEquals(DatalakeBackupStatusResponse.NO_FAILURES, response.getFailureReason());
        assertTrue(response.isComplete());
        Assertions.assertThat(response.getIncludedData()).contains("ATLAS_METADATA", "HMS_METADATA", "RANGER_AUDITS", "RANGER_PERMISSIONS");
    }

    @Test
    void testBackupContainsRangerAudits() {
        datalakeDRProto.BackupRestoreOperationStatus.Builder status = datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus("SUCCESSFUL");
        datalakeDRProto.SolrBackupRestoreState.Builder solr = datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setRangerAuditsCollection(status);

        datalakeDRProto.InternalBackupRestoreState.Builder backupState = datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solr);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder = datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(SUCCESSFUL_STATE)
                .setOperationStates(backupState.build());

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());

        Assertions.assertThat(response.getIncludedData()).containsOnly("RANGER_AUDITS");
    }

    @Test
    void testBackupContainsAtlasMetadata() {
        datalakeDRProto.BackupRestoreOperationStatus.Builder status = datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus("SUCCESSFUL");
        datalakeDRProto.SolrBackupRestoreState.Builder solr = datalakeDRProto.SolrBackupRestoreState.newBuilder()
                .setFulltextIndexCollection(status)
                .setVertexIndexCollection(status)
                .setEdgeIndexCollection(status);

        datalakeDRProto.HbaseBackupRestoreState.Builder hbase = datalakeDRProto.HbaseBackupRestoreState.newBuilder()
                .setAtlasJanusTable(datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus(SUCCESSFUL_STATE).build())
                .setAtlasEntityAuditEventTable(datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus(SUCCESSFUL_STATE).build());


        datalakeDRProto.InternalBackupRestoreState.Builder backupMetadata = datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setHbase(hbase);
        datalakeDRProto.InternalBackupRestoreState.Builder backupIndexes = datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solr);
        datalakeDRProto.InternalBackupRestoreState.Builder backupAll = datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setSolr(solr)
                .setHbase(hbase);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder = datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(SUCCESSFUL_STATE)
                .setOperationStates(backupMetadata.build());

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        Assertions.assertThat(response.getIncludedData()).containsOnly("ATLAS_METADATA");

        builder.setOperationStates(backupIndexes.build());
        response = underTest.convert(builder.build());
        Assertions.assertThat(response.getIncludedData()).containsOnly("ATLAS_INDEXES");

        builder.setOperationStates(backupAll.build());
        response = underTest.convert(builder.build());
        Assertions.assertThat(response.getIncludedData()).contains("ATLAS_INDEXES", "ATLAS_INDEXES");
    }

    @Test
    void testBackupContainsPermissions() {
        datalakeDRProto.BackupRestoreOperationStatus.Builder status = datalakeDRProto.BackupRestoreOperationStatus.newBuilder().setStatus("SUCCESSFUL");
        datalakeDRProto.DatabaseBackupRestoreState.Builder database = datalakeDRProto.DatabaseBackupRestoreState.newBuilder().setDatabase(status);


        datalakeDRProto.InternalBackupRestoreState.Builder backupState = datalakeDRProto.InternalBackupRestoreState.newBuilder()
                .setDatabase(database);
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder = datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                .setOverallState(SUCCESSFUL_STATE)
                .setOperationStates(backupState.build());

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        Assertions.assertThat(response.getIncludedData()).containsOnly("HMS_METADATA", "RANGER_PERMISSIONS");
    }

    @Test
    void testInProgressBackup() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
                datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                        .setOverallState("IN_PROGRESS");

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.IN_PROGRESS, response.getState());
        assertFalse(response.isComplete());
    }

    @Test
    void testFailedBackupAdminOperations() {
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

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());

        Assertions.assertThat(response.getFailureReason()).contains(FAILURE_REASON);
        Assertions.assertThat(response.getFailureReason()).contains(FAILURE_REASON2);
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedHbaseBackupSameFailureReason() {
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
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedHbaseBackupDifferentFailureReasons() {
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
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedSolrBackupSameFailureReason() {
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
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedSolrBackupDifferentFailureReasons() {
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
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedDatabaseBackup() {
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
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());
        assertEquals(expectedFailure, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testInvalidBackupStatus() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
                datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                        .setOverallState("INVALID_STATUS");

        assertThrows(IllegalArgumentException.class, () -> underTest.convert(builder.build()));
    }

    @Test
    void testSuccessfulRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
            datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                .setOverallState("SUCCESSFUL");

        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.SUCCESSFUL, response.getState());
        assertEquals(DatalakeBackupStatusResponse.NO_FAILURES, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testInProgressRestore() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                        .setOverallState("IN_PROGRESS");

        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.IN_PROGRESS, response.getState());
        assertFalse(response.isComplete());
    }

    @Test
    void testFailedRestoreAdminOperations() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedHbaseRestoreSameFailureReason() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedHbaseRestoreDifferentFailureReasons() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedSolrRestoreSameFailureReason() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedSolrRestoreDifferentFailureReasons() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedSolrRestoreSameFailureReasonWithDelete() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedSolrRestoreSameFailureReasonWithDeleteDifferentReasons() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testFailedDatabaseRestore() {
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
        assertTrue(response.isComplete());
    }

    @Test
    void testInvalidRestoreStatus() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                        .setOverallState("INVALID_STATUS");

        assertThrows(IllegalArgumentException.class, () -> underTest.convert(builder.build()));
    }

    @Test
    void testBackupLegacyFailureReason() {
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder =
                datalakeDRProto.BackupDatalakeStatusResponse.newBuilder()
                        .setOverallState(FAILED_STATE)
                        .setFailureReason(FAILURE_REASON);

        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.FAILED, response.getState());
        assertEquals(FAILURE_REASON, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testRestoreLegacyFailureReason() {
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder()
                        .setOverallState(FAILED_STATE)
                        .setFailureReason(FAILURE_REASON);

        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeRestoreStatusResponse.State.FAILED, response.getState());
        assertEquals(FAILURE_REASON, response.getFailureReason());
        assertTrue(response.isComplete());
    }

    @Test
    void testPreCheckBackupFailed() {
        datalakeDRProto.BackupDatalakeResponse.Builder builder =
                datalakeDRProto.BackupDatalakeResponse.newBuilder()
                        .setOverallState(VALIDATION_FAILED_STATE)
                        .setFailureReason(PRECHECKS_FAILED_REASON);
        DatalakeBackupStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.VALIDATION_FAILED, response.getState());
        assertEquals(PRECHECKS_FAILED_REASON, response.getFailureReason());
    }

    @Test
    void testPreCheckRestoreFailed() {
        datalakeDRProto.RestoreDatalakeResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeResponse.newBuilder()
                        .setOverallState(VALIDATION_FAILED_STATE)
                        .setFailureReason(PRECHECKS_FAILED_REASON);
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.VALIDATION_FAILED, response.getState());
        assertEquals(PRECHECKS_FAILED_REASON, response.getFailureReason());
    }

    @Test
    void testPrecheckBackupSuccess() {
        datalakeDRProto.RestoreDatalakeResponse.Builder builder =
                datalakeDRProto.RestoreDatalakeResponse.newBuilder()
                        .setOverallState("SUCCESSFUL");
        DatalakeRestoreStatusResponse response = underTest.convert(builder.build());
        assertEquals(DatalakeOperationStatus.State.SUCCESSFUL, response.getState());

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
