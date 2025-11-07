package com.sequenceiq.externalizedcompute.service.exception;

import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;

public class ExternalizedComputeClusterStatusUpdateException extends RuntimeException {

    public ExternalizedComputeClusterStatusUpdateException(String from, ExternalizedComputeClusterStatusEnum to) {
        super(String.format("Can't update Externalized Compute Cluster status from %s to %s", from, to));
    }
}
