package com.sequenceiq.cloudbreak.service.datalake;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class DataLakeStatusCheckerService {

    @Inject
    private SdxClientService sdxClientService;

    public void validateRunningState(StackView stack) {
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

    public void validateAvailableState(StackView stack) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            sdxClientService
                    .getByEnvironmentCrn(stack.getEnvironmentCrn())
                    .forEach(sdxClusterResponse -> {
                        SdxClusterStatusResponse status = sdxClusterResponse.getStatus();
                        if (!(status.isRollingUpgradeInProgress() || status.isAvailable())) {
                            throw new BadRequestException(String.format("This action requires the Data Lake to be available, " +
                                    "but the status is '%s', Reason: '%s'.",
                                    sdxClusterResponse.getStatus().name(),
                                    Objects.toString(sdxClusterResponse.getStatusReason(), "")));
                        }
                    });
        }
    }
}
