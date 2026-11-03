package com.sequenceiq.cloudbreak.service.datalake;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperationValidationView;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperations;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class DataLakeStatusCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeStatusCheckerService.class);

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

    /**
     * Data Hub operations (upgrade, upscale, repair) fetch the Remote Data Context from the Data Lake's Cloudera Manager late in their flows.
     * Probing the same call up-front lets these operations fail fast, before any parcel transfer or infrastructure change.
     * A previously fetched Remote Data Context is accepted, so a transient outage still lets them proceed in degraded mode;
     * only the case where no usable Remote Data Context exists at all is rejected.
     */
    public void validateDatalakeReachable(StackView stack) {
        if (StackType.WORKLOAD.equals(stack.getType())) {
            Optional<String> datalakeCrn = getDatalakeCrn(stack);
            if (datalakeCrn.isPresent()) {
                try {
                    LOGGER.info("Validating that the Remote Data Context can be fetched from Data Lake {} for {}", datalakeCrn.get(), stack.getName());
                    platformAwareSdxConnector.getRdcView(datalakeCrn.get());
                } catch (Exception e) {
                    LOGGER.warn("No usable Remote Data Context is available from Data Lake {} for {}", datalakeCrn.get(), stack.getName(), e);
                    throw new DatalakeNotReachableException("The Data Lake's Cloudera Manager is not reachable and no previously fetched "
                            + "data is available, but it is required for this operation. Please check the Data Lake state and try again. Cause: "
                            + e.getMessage(), e);
                }
            } else {
                LOGGER.info("No Data Lake found for {}, skipping Data Lake reachability validation", stack.getName());
            }
        }
    }

    /**
     * Resolves the Data Lake of the given Data Hub: the one referenced by the stack, or else the one found by the environment.
     */
    public Optional<String> getDatalakeCrn(StackView stack) {
        if (StringUtils.isNotEmpty(stack.getDatalakeCrn())) {
            return Optional.of(stack.getDatalakeCrn());
        }
        return platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn())
                .map(SdxBasicView::crn)
                .filter(StringUtils::isNotEmpty);
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
