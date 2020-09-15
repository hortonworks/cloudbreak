package com.sequenceiq.cloudbreak.datalakedr.converter;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto;
import com.sequenceiq.cloudbreak.datalakedr.model.DatalakeDrStatusResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GrpcStatusResponseToDatalakeDrStatusResponseConverter {

    public DatalakeDrStatusResponse convert(datalakeDRProto.BackupDatalakeStatusResponse response) {
        return new DatalakeDrStatusResponse(
            DatalakeDrStatusResponse.State.valueOf(response.getOverallState()),
            Optional.ofNullable(response.getFailureReason())
        );
    }

    public DatalakeDrStatusResponse convert(datalakeDRProto.RestoreDatalakeStatusResponse response) {
        return new DatalakeDrStatusResponse(
            DatalakeDrStatusResponse.State.valueOf(response.getOverallState()),
            Optional.ofNullable(response.getFailureReason())
        );
    }
}
