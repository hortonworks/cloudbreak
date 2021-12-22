package com.sequenceiq.cloudbreak.datalakedr.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeBackupStatusResponse;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeRestoreStatusResponse;

@Component
public class GrpcStatusResponseToDatalakeBackupRestoreStatusResponseConverter {

    public DatalakeBackupStatusResponse convert(datalakeDRProto.BackupDatalakeResponse response) {
        return new DatalakeBackupStatusResponse(response.getBackupId(),
                DatalakeBackupStatusResponse.State.valueOf(response.getOverallState()),
                Optional.ofNullable(response.getFailureReason())
        );
    }

    public DatalakeRestoreStatusResponse convert(datalakeDRProto.RestoreDatalakeResponse response) {
        return new DatalakeRestoreStatusResponse(response.getBackupId(), response.getRestoreId(),
                DatalakeRestoreStatusResponse.State.valueOf(response.getOverallState()),
                Optional.ofNullable(response.getFailureReason())
        );
    }

    public DatalakeBackupStatusResponse convert(datalakeDRProto.BackupDatalakeStatusResponse response) {
        return new DatalakeBackupStatusResponse(response.getBackupId(),
            DatalakeBackupStatusResponse.State.valueOf(response.getOverallState()),
            Optional.ofNullable(response.getFailureReason())
        );
    }

    public DatalakeRestoreStatusResponse convert(datalakeDRProto.RestoreDatalakeStatusResponse response) {
        return new DatalakeRestoreStatusResponse(response.getBackupId(), response.getRestoreId(),
                DatalakeRestoreStatusResponse.State.valueOf(response.getOverallState()),
            Optional.ofNullable(response.getFailureReason())
        );
    }
}
