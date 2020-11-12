package com.sequenceiq.cloudbreak.service.datalake;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class DataLakeStatusCheckerService {

    @Inject
    private SdxClientService sdxClientService;

    public void validateRunningState(Stack stack) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            List<SdxClusterResponse> sdxClusterResponses = sdxClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            sdxClusterResponses.forEach(sdxClusterResponse -> validateState(sdxClusterResponse, SdxClusterStatusResponse.RUNNING));
        }
    }

    public void validateState(SdxClusterResponse sdxCluster, SdxClusterStatusResponse expectedStatus) {
        if (!expectedStatus.equals(sdxCluster.getStatus())) {
            throw new BadRequestException("This action requires the Data Lake to be available, but the status is " + sdxCluster.getStatusReason());
        }
    }
}
