package com.sequenceiq.thunderhead.grpc.service.datalakedr;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRGrpc;
import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;

import io.grpc.stub.StreamObserver;


/**
 * Provides mock backup and restore interactions.
 *
 * Both the backup and restore processes are advanced by calling their respective {@code *status()} methods.
 * The lifecycle of a mocked backup or restore is:
 * <ol>
 *     <li>Create in the state {@code STARTED} with an invocation of {@code backupDatalake} or {@code restoreDatalake}</li>
 *     <li>Advance to {@code IN_PROGRESS}, with a request to get the status</li>
 *     <li>Advance to {@code SUCCESFFUL}, with another request to get the status</li>
 * </ol>
 */
@Component
public class MockDatalakeDrService extends datalakeDRGrpc.datalakeDRImplBase {

    private static Map<String, DatalakeBackupStatusResponse.State> mockStatusDatabase = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(MockDatalakeDrService.class);

    @Override
    public void backupDatalake(datalakeDRProto.BackupDatalakeRequest request, StreamObserver<datalakeDRProto.BackupDatalakeResponse> responseObserver) {
        LOGGER.info("Backing up for {}", request.getBackupName());
        UUID id = UUID.randomUUID();
        mockStatusDatabase.put(id.toString(), DatalakeBackupStatusResponse.State.STARTED);
        responseObserver.onNext(datalakeDRProto.BackupDatalakeResponse.newBuilder()
                .setBackupName(request.getBackupName())
                .setBackupId(id.toString())
                .setOverallState(DatalakeBackupStatusResponse.State.STARTED.name()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void restoreDatalake(datalakeDRProto.RestoreDatalakeRequest request, StreamObserver<datalakeDRProto.RestoreDatalakeResponse> responseObserver) {

        LOGGER.info("restore for {}", request.getBackupName());
        UUID requestId = UUID.randomUUID();
        mockStatusDatabase.put(requestId.toString(), DatalakeBackupStatusResponse.State.STARTED);
        responseObserver.onNext(datalakeDRProto.RestoreDatalakeResponse.newBuilder()
                .setBackupId((Strings.isNullOrEmpty(request.getBackupId())) ? UUID.randomUUID().toString() : request.getBackupId())
                .setRestoreId(requestId.toString())
                .setOverallState(DatalakeBackupStatusResponse.State.STARTED.name()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void backupDatalakeStatus(datalakeDRProto.BackupDatalakeStatusRequest request,
            StreamObserver<datalakeDRProto.BackupDatalakeStatusResponse> responseObserver) {
        DatalakeBackupStatusResponse.State state = mockStatusDatabase.get(request.getBackupId());
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder = datalakeDRProto.BackupDatalakeStatusResponse.newBuilder();
        builder.setBackupId(request.getBackupId());
        switch (state) {
            case STARTED:
                mockStatusDatabase.put(request.getBackupId(), DatalakeBackupStatusResponse.State.IN_PROGRESS);
                builder.setOverallState(DatalakeBackupStatusResponse.State.IN_PROGRESS.name());
                break;
            case IN_PROGRESS:
                mockStatusDatabase.put(request.getBackupId(), DatalakeBackupStatusResponse.State.SUCCESSFUL);
                builder.setOverallState(DatalakeBackupStatusResponse.State.SUCCESSFUL.name());
                break;
            default:
                builder.setOverallState(state.name());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void restoreDatalakeStatus(datalakeDRProto.RestoreDatalakeStatusRequest request,
            StreamObserver<datalakeDRProto.RestoreDatalakeStatusResponse> responseObserver) {
        DatalakeBackupStatusResponse.State state = mockStatusDatabase.get(request.getRestoreId());
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder = datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder();
        builder.setRestoreId(request.getRestoreId());
        switch (state) {
            case STARTED:
                mockStatusDatabase.put(request.getRestoreId(), DatalakeBackupStatusResponse.State.IN_PROGRESS);
                builder.setOverallState(DatalakeBackupStatusResponse.State.IN_PROGRESS.name());
                break;
            case IN_PROGRESS:
                mockStatusDatabase.put(request.getRestoreId(), DatalakeBackupStatusResponse.State.SUCCESSFUL);
                builder.setOverallState(DatalakeBackupStatusResponse.State.SUCCESSFUL.name());
                break;
            default:
                builder.setOverallState(state.name());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
