package com.sequenceiq.cloudbreak.datalakedr.converter;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupDatalakeStatusResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.BackupRestoreOperationStatus;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.HbaseBackupRestoreState;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.InternalBackupRestoreState;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.RestoreDatalakeStatusResponse;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.SolrBackupRestoreState;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;

@Component
public class GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter {

    static final String FAILED_STATE = "FAILED";

    static final String VALIDATION_FAILED_STATE = "VALIDATION_FAILED";

    private static final Set<String> FAILED_STATES = Set.of(FAILED_STATE, VALIDATION_FAILED_STATE);

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter.class);

    public DatalakeBackupStatusResponse convert(BackupDatalakeResponse response) {
        return new DatalakeBackupStatusResponse(response.getBackupId(),
                DatalakeBackupStatusResponse.State.valueOf(response.getOverallState()),
                Optional.ofNullable(parseFailuresFromOperationsStates(response.getOperationStates(), response.getFailureReason()))
        );
    }

    public DatalakeRestoreStatusResponse convert(RestoreDatalakeResponse response) {
        return new DatalakeRestoreStatusResponse(response.getBackupId(), response.getRestoreId(),
                DatalakeRestoreStatusResponse.State.valueOf(response.getOverallState()),
                Optional.ofNullable(parseFailuresFromOperationsStates(response.getOperationStates(), response.getFailureReason()))
        );
    }

    public DatalakeBackupStatusResponse convert(BackupDatalakeStatusResponse response) {
        return new DatalakeBackupStatusResponse(response.getBackupId(),
            DatalakeBackupStatusResponse.State.valueOf(response.getOverallState()),
            Optional.ofNullable(parseFailuresFromOperationsStates(response.getOperationStates(), response.getFailureReason()))
        );
    }

    public DatalakeRestoreStatusResponse convert(RestoreDatalakeStatusResponse response) {
        return new DatalakeRestoreStatusResponse(response.getBackupId(), response.getRestoreId(),
                DatalakeRestoreStatusResponse.State.valueOf(response.getOverallState()),
            Optional.ofNullable(parseFailuresFromOperationsStates(response.getOperationStates(), response.getFailureReason()))
        );
    }

    private String parseFailuresFromOperationsStates(InternalBackupRestoreState operationStates, String legacyFailureReason) {
        String failure;
        if (operationStates != null && !operationStates.equals(InternalBackupRestoreState.getDefaultInstance())) {
            List<String> failures = new LinkedList<>();
            getFailure(OperationEnum.STOP_SERVICES.description(), operationStates.getAdminOperations().getStopServices())
                    .ifPresent(failures::add);
            getFailure(OperationEnum.START_SERVICES.description(), operationStates.getAdminOperations().getStartServices())
                    .ifPresent(failures::add);
            parseHbaseFailure(operationStates.getHbase(), failures);
            parseSolrFailure(operationStates.getSolr(), failures);
            getFailure(OperationEnum.DATABASE.description(), operationStates.getDatabase().getDatabase())
                    .ifPresent(failures::add);
            getFailure(OperationEnum.STORAGE_PERMISSION_VALIDATION_PRECHECK.description(), operationStates.getAdminOperations().getPrecheckStoragePermission())
                    .ifPresent(failures::add);
            getFailure(OperationEnum.RANGER_AUDIT_VALIDATION_PRECHECK.description(), operationStates.getAdminOperations().getPrecheckRangerAuditValidation())
                    .ifPresent(failures::add);
            failure = failures.isEmpty() ? null : String.join(", ", failures);
        } else {
            failure = StringUtils.isNotBlank(legacyFailureReason) ? legacyFailureReason : null;
        }

        if (StringUtils.isNotBlank(failure)) {
            LOGGER.error("Found error on backup/restore operation: {}", failure);
        }
        return failure;
    }

    private void parseHbaseFailure(HbaseBackupRestoreState hbase, List<String> failures) {
        if (hbase != null) {
            List<BackupRestoreOperationStatus> allFailureReasons = List.of(
                    hbase.getAtlasJanusTable(),
                    hbase.getAtlasEntityAuditEventTable()
            );
            if (StringUtils.isNotEmpty(hbase.getAtlasJanusTable().getFailureReason()) && areAllFailuresTheSame(allFailureReasons)) {
                getFailure(OperationEnum.HBASE.description(), hbase.getAtlasJanusTable())
                        .ifPresent(failures::add);
            } else {
                getFailure(OperationEnum.HBASE_ATLAS_JANUS.description(), hbase.getAtlasJanusTable())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.HBASE_ATLAS_AUDIT.description(), hbase.getAtlasEntityAuditEventTable())
                        .ifPresent(failures::add);
            }
        }
    }

    private void parseSolrFailure(SolrBackupRestoreState solr, List<String> failures) {
        if (solr != null) {
            List<BackupRestoreOperationStatus> allFailureReasons = List.of(
                    solr.getEdgeIndexCollection(),
                    solr.getFulltextIndexCollection(),
                    solr.getRangerAuditsCollection(),
                    solr.getVertexIndexCollection()
            );
            if (StringUtils.isNotEmpty(solr.getEdgeIndexCollection().getFailureReason()) && areAllFailuresTheSame(allFailureReasons)) {
                getFailure(OperationEnum.SOLR.description(), solr.getEdgeIndexCollection())
                        .ifPresent(failures::add);
            } else {
                getFailure(OperationEnum.SOLR_EDGE_INDEX.description(), solr.getEdgeIndexCollection())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.SOLR_FULLTEXT_INDEX.description(), solr.getFulltextIndexCollection())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.SOLR_RANGER_AUDITS.description(), solr.getRangerAuditsCollection())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.SOLR_VERTEX_INDEX.description(), solr.getVertexIndexCollection())
                        .ifPresent(failures::add);
            }
            List<BackupRestoreOperationStatus> allDeleteFailureReasons = List.of(
                    solr.getEdgeIndexCollectionDelete(),
                    solr.getFulltextIndexCollectionDelete(),
                    solr.getRangerAuditsCollectionDelete(),
                    solr.getVertexIndexCollectionDelete()
            );
            if (StringUtils.isNotEmpty(solr.getEdgeIndexCollectionDelete().getFailureReason()) && areAllFailuresTheSame(allDeleteFailureReasons)) {
                getFailure(OperationEnum.SOLR_DELETE.description(), solr.getEdgeIndexCollectionDelete())
                        .ifPresent(failures::add);
            } else {
                getFailure(OperationEnum.SOLR_EDGE_INDEX_DELETE.description(), solr.getEdgeIndexCollectionDelete())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.SOLR_FULLTEXT_INDEX_DELETE.description(), solr.getFulltextIndexCollectionDelete())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.SOLR_RANGER_AUDITS_DELETE.description(), solr.getRangerAuditsCollectionDelete())
                        .ifPresent(failures::add);
                getFailure(OperationEnum.SOLR_VERTEX_INDEX_DELETE.description(), solr.getVertexIndexCollectionDelete())
                        .ifPresent(failures::add);
            }
        }
    }

    private static Optional<String> getFailure(String operationName, BackupRestoreOperationStatus status) {
        return Optional.ofNullable(status)
                .filter(GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter::isFailed)
                .map(it -> operationName + ": " + it.getFailureReason());
    }

    private static boolean isFailed(BackupRestoreOperationStatus status) {
        return FAILED_STATES.contains(status.getStatus());
    }

    private boolean areAllFailuresTheSame(List<BackupRestoreOperationStatus> statuses) {
        return statuses.stream()
                .filter(Objects::nonNull)
                .map(BackupRestoreOperationStatus::getFailureReason)
                .allMatch(failureReason -> statuses.get(0).getFailureReason().equals(failureReason));
    }
}
