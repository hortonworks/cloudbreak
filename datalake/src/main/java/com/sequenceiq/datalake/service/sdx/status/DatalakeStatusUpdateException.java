package com.sequenceiq.datalake.service.sdx.status;

import com.sequenceiq.datalake.entity.DatalakeStatusEnum;

public class DatalakeStatusUpdateException extends RuntimeException {

    public DatalakeStatusUpdateException(DatalakeStatusEnum from, DatalakeStatusEnum to) {
        super(String.format("Can't update Datalake status from %s to %s", from, to));
    }
}
