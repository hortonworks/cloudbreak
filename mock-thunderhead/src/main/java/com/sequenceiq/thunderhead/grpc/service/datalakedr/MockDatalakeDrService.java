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
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeOperationStatus;

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

    private static Map<String, DatalakeOperationStatus.State> mockStatusDatabase = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(MockDatalakeDrService.class);

    @Override
    public void backupDatalake(datalakeDRProto.BackupDatalakeRequest request, StreamObserver<datalakeDRProto.BackupDatalakeResponse> responseObserver) {
        LOGGER.info("Backing up for {}", request.getBackupName());
        String backupLocaiton = request.getBackupLocation();
        UUID id = UUID.randomUUID();
        if (!backupLocaiton.contains("/cancel")) {
            mockStatusDatabase.put(id.toString(), DatalakeOperationStatus.State.STARTED);
            responseObserver.onNext(datalakeDRProto.BackupDatalakeResponse.newBuilder()
                    .setBackupName(request.getBackupName())
                    .setBackupId(id.toString())
                    .setOverallState(DatalakeOperationStatus.State.STARTED.name()).build());
        } else {
            mockStatusDatabase.put(id.toString(), DatalakeOperationStatus.State.CANCELLED);
            responseObserver.onNext(datalakeDRProto.BackupDatalakeResponse.newBuilder()
                    .setBackupName(request.getBackupName())
                    .setBackupId(id.toString())
                    .setOverallState(DatalakeOperationStatus.State.CANCELLED.name()).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void restoreDatalake(datalakeDRProto.RestoreDatalakeRequest request, StreamObserver<datalakeDRProto.RestoreDatalakeResponse> responseObserver) {

        LOGGER.info("restore for {}", request.getBackupName());
        UUID requestId = UUID.randomUUID();
        mockStatusDatabase.put(requestId.toString(), DatalakeOperationStatus.State.STARTED);
        responseObserver.onNext(datalakeDRProto.RestoreDatalakeResponse.newBuilder()
                .setBackupId((Strings.isNullOrEmpty(request.getBackupId())) ? UUID.randomUUID().toString() : request.getBackupId())
                .setRestoreId(requestId.toString())
                .setOverallState(DatalakeOperationStatus.State.STARTED.name()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void backupDatalakeStatus(datalakeDRProto.BackupDatalakeStatusRequest request,
            StreamObserver<datalakeDRProto.BackupDatalakeStatusResponse> responseObserver) {
        DatalakeOperationStatus.State state = mockStatusDatabase.get(request.getBackupId());
        datalakeDRProto.BackupDatalakeStatusResponse.Builder builder = datalakeDRProto.BackupDatalakeStatusResponse.newBuilder();
        builder.setBackupId(request.getBackupId());

        if (state == null) {
            mockStatusDatabase.put(request.getBackupId(), DatalakeOperationStatus.State.SUCCESSFUL);
            builder.setOverallState(DatalakeOperationStatus.State.SUCCESSFUL.name());
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
            return;
        }
        switch (state) {
            case STARTED:
                mockStatusDatabase.put(request.getBackupId(), DatalakeOperationStatus.State.IN_PROGRESS);
                builder.setOverallState(DatalakeOperationStatus.State.IN_PROGRESS.name());
                break;
            case IN_PROGRESS:
                mockStatusDatabase.put(request.getBackupId(), DatalakeOperationStatus.State.SUCCESSFUL);
                builder.setOverallState(DatalakeOperationStatus.State.SUCCESSFUL.name());
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
        DatalakeOperationStatus.State state = mockStatusDatabase.get(request.getRestoreId());
        datalakeDRProto.RestoreDatalakeStatusResponse.Builder builder = datalakeDRProto.RestoreDatalakeStatusResponse.newBuilder();
        builder.setRestoreId(request.getRestoreId());
        switch (state) {
            case STARTED:
                mockStatusDatabase.put(request.getRestoreId(), DatalakeOperationStatus.State.IN_PROGRESS);
                builder.setOverallState(DatalakeOperationStatus.State.IN_PROGRESS.name());
                break;
            case IN_PROGRESS:
                mockStatusDatabase.put(request.getRestoreId(), DatalakeOperationStatus.State.SUCCESSFUL);
                builder.setOverallState(DatalakeOperationStatus.State.SUCCESSFUL.name());
                break;
            default:
                builder.setOverallState(state.name());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
