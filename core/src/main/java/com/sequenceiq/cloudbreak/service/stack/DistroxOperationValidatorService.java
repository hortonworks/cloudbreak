package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.conf.DatahubOperationConfig;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperationValidationView;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperations;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class DistroxOperationValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxOperationValidatorService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DatahubOperationConfig datahubOperationConfig;

    public DistroXOperationValidationView validateDistroXStartOperation(String environmentCrn, DistroXOperations distroXOperation,
            List<SdxClusterResponse> sdxClusterResponses, List<InstanceMetadataView> instanceMetadataViews) {
        DistroXOperationValidationView distroXOperationValidationView = new DistroXOperationValidationView();
        distroXOperationValidationView.setOperation(distroXOperation);

        if (sdxClusterResponses.isEmpty()) {
            distroXOperationValidationView.setAllowed(false);
            distroXOperationValidationView.setReason(String.format("Could not find the datalake associated " +
                    "with the environment with crn: '%s'", environmentCrn));
            return distroXOperationValidationView;
        }

        if (entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(ThreadBasedUserCrnProvider.getAccountId()) &&
                (sdxClusterResponses.getFirst().getClusterShape().equals(SdxClusterShape.ENTERPRISE) ||
                        sdxClusterResponses.getFirst().getClusterShape().equals(SdxClusterShape.ENTERPRISE_WITHOUT_HBASE))) {
            LOGGER.info("Validating Start of datahub based on primary gateway health of datalake");
            boolean primaryGatewayHealthy = isPrimaryGatewayHealthy(instanceMetadataViews);
            distroXOperationValidationView.setAllowed(primaryGatewayHealthy);
            if (!primaryGatewayHealthy) {
                distroXOperationValidationView.setReason(String.format("Instance health check failed for SDX cluster '%s'. Details: " +
                        "Primary gateway of data lake is not healthy.", sdxClusterResponses.getFirst().getName()));
            }
            return distroXOperationValidationView;
        }

        distroXOperationValidationView.setAllowed(sdxClusterResponses.getFirst().getStatus().isAvailable()
                || sdxClusterResponses.getFirst().getStatus().isRollingUpgradeInProgress());
        return distroXOperationValidationView;
    }

    private boolean isPrimaryGatewayHealthy(List<InstanceMetadataView> instanceMetadataViews) {
        return instanceMetadataViews.stream()
                .anyMatch(instanceMetaDataView ->
                        InstanceMetadataType.GATEWAY_PRIMARY.equals(instanceMetaDataView.getInstanceMetadataType())
                                && (InstanceStatus.SERVICES_HEALTHY.equals(instanceMetaDataView.getInstanceStatus())
                                || InstanceStatus.SERVICES_RUNNING.equals(instanceMetaDataView.getInstanceStatus())));
    }

    public DistroXOperationValidationView validateDistroXCreateOperation(String envCrn, DistroXOperations distroXOperation,
            List<SdxClusterResponse> sdxClusterResponses, List<InstanceMetadataView> instanceMetadataViews) {
        DistroXOperationValidationView distroXOperationValidationView = new DistroXOperationValidationView();
        distroXOperationValidationView.setOperation(distroXOperation);
        if (sdxClusterResponses.isEmpty()) {
            distroXOperationValidationView.setAllowed(false);
            distroXOperationValidationView.setReason(String.format("Could not find the datalake associated " +
                    "with the environment with crn: '%s'", envCrn));
            return distroXOperationValidationView;
        }
        if (entitlementService.isValidateDistroxOperationsBySdxHealthEnabled(ThreadBasedUserCrnProvider.getAccountId()) &&
                (sdxClusterResponses.getFirst().getClusterShape().equals(SdxClusterShape.ENTERPRISE) ||
                        sdxClusterResponses.getFirst().getClusterShape().equals(SdxClusterShape.ENTERPRISE_WITHOUT_HBASE))) {

            LOGGER.info("Validating Creation of datahub based on datalake instance health");
            String sdxClusterName = sdxClusterResponses.getFirst().getName();

            //contains groups that have at least one healthy instance
            Set<String> groupsWithAnyHealthyInstance = getHostGroupsByHealthState(instanceMetadataViews, true);

            //contains groups that have at least one unhealthy instance
            Set<String> groupsWithAnyUnhealthyInstance = getHostGroupsByHealthState(instanceMetadataViews, false);
            DatahubOperationConfig.OperationConfig operationConfig = datahubOperationConfig.getOperations().get(distroXOperation.name().toLowerCase());
            Set<String> unhealthyMandatoryGroups = operationConfig
                    .getMandatoryHealthyHostgroups()
                    .stream()
                    .filter(groupsWithAnyUnhealthyInstance::contains)
                    .collect(Collectors.toSet());
            Set<String> missingHealthyGroups = operationConfig
                    .getRequiredPartialHostgroups()
                    .stream()
                    .filter(hostGroup -> !groupsWithAnyHealthyInstance.contains(hostGroup))
                    .collect(Collectors.toSet());

            if (unhealthyMandatoryGroups.isEmpty() && missingHealthyGroups.isEmpty()) {
                distroXOperationValidationView.setAllowed(true);
                return distroXOperationValidationView;
            }

            List<String> errorDetails = new ArrayList<>();
            if (!unhealthyMandatoryGroups.isEmpty()) {
                errorDetails.add(String.format("Mandatory host groups containing unhealthy instances: '%s'",
                        String.join(", ", unhealthyMandatoryGroups)));
            }
            if (!missingHealthyGroups.isEmpty()) {
                errorDetails.add(String.format("Host group(s) without at least one healthy instance: '%s'", String.join(", ", missingHealthyGroups)));
            }

            String reason = String.format("Instance health check failed for SDX cluster '%s'. Details: %s.",
                    sdxClusterName, String.join("; ", errorDetails));

            distroXOperationValidationView.setReason(reason);
            LOGGER.info(reason);
            distroXOperationValidationView.setAllowed(false);
            return distroXOperationValidationView;
        }
        distroXOperationValidationView.setAllowed(sdxClusterResponses.getFirst().getStatus().isRunning());
        return distroXOperationValidationView;
    }

    private Set<String> getHostGroupsByHealthState(List<InstanceMetadataView> instanceMetadataViews, boolean requireHealthyHostGroups) {
        return instanceMetadataViews.stream()
                .filter(instanceMetadataView ->
                        requireHealthyHostGroups == SERVICES_HEALTHY.equals(instanceMetadataView.getInstanceStatus()))
                .map(InstanceMetadataView::getInstanceGroupName)
                .collect(Collectors.toSet());
    }
}
