package com.sequenceiq.cloudbreak.service.datalake;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperationValidationView;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperations;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class DataLakeStatusCheckerService {

    @Inject
    private SdxClientService sdxClientService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public void validateRunningState(StackView stack) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            List<SdxClusterResponse> sdxClusterResponses = sdxClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
            sdxClusterResponses.forEach(sdxClusterResponse -> validateState(sdxClusterResponse, SdxClusterStatusResponse.RUNNING));
        }
    }

    public void validateScaleOperationBasedOnDatalake(StackType stackType, String environmentCrn, boolean upscale) {
        if (StackType.WORKLOAD.equals(stackType) && upscale) {
            List<DistroXOperationValidationView> distroXOperationValidationView =
                    platformAwareSdxConnector.validateDistroxOperations(environmentCrn);
            DistroXOperationValidationView distroXOperationValidationViewResponse = distroXOperationValidationView.stream()
                    .filter(i -> DistroXOperations.SCALE.equals(i.getOperation()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(String.format("Validation result for operation '%s' was not found.",
                            DistroXOperations.SCALE.name())));
            if (!distroXOperationValidationViewResponse.isAllowed()) {
                throw new BadRequestException(String.format("Data Hub scaling is not allowed due to Data Lake being unavailable. Reason: '%s'.",
                        Objects.toString(distroXOperationValidationViewResponse.getReason(), "")));
            }
        }
    }

    public void validateState(SdxClusterResponse sdxCluster, SdxClusterStatusResponse expectedStatus) {
        if (!expectedStatus.equals(sdxCluster.getStatus())) {
            throw new BadRequestException("This action requires the Data Lake to be available, but the status is " + sdxCluster.getStatusReason());
        }
    }

    public void validateStartOperationBasedOnDatalake(StackView stack) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            List<DistroXOperationValidationView> distroXOperationValidationView =
                    platformAwareSdxConnector.validateDistroxOperations(stack.getEnvironmentCrn());
            DistroXOperationValidationView distroXOperationValidationViewResponse = distroXOperationValidationView.stream()
                    .filter(i -> DistroXOperations.START.equals(i.getOperation()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(String.format("Validation result for operation '%s' was not found.",
                            DistroXOperations.START.name())));
            if (!distroXOperationValidationViewResponse.isAllowed()) {
                throw new BadRequestException(String.format("Data Hub start is not allowed due to Data Lake being unavailable. Reason: '%s'.",
                        Objects.toString(distroXOperationValidationViewResponse.getReason(), "")));
            }
        }
    }
}
