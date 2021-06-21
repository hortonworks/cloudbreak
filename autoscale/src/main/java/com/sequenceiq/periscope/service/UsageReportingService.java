package com.sequenceiq.periscope.service;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.domain.BaseAlert;
import com.sequenceiq.periscope.domain.Cluster;

@Service
public class UsageReportingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageReportingService.class);

    @Inject
    private UsageReporter usageReporter;

    @Async
    public void reportAutoscalingTriggered(int adjustmentCount, int clusterSize, ScalingStatus scalingStatus, String scalingAction,
            BaseAlert alert, Cluster cluster) {
        UsageProto.CDPDatahubAutoscaleTriggered.Builder cdpAutoscaleTriggerDetailsBuilder =
                UsageProto.CDPDatahubAutoscaleTriggered.newBuilder().setAutoscaleTriggerDetails(
                        UsageProto.CDPAutoscaleTriggerDetails.newBuilder()
                                .setAccountId(cluster.getClusterPertain().getTenant())
                                .setClusterCrn(cluster.getStackCrn())
                                .setClusterName(defaultIfEmpty(cluster.getStackName(), ""))
                                .setAutoscalingAction(scalingAction)
                                .setAutoscaleStatus(getAutoscaleStatusType(scalingStatus))
                                .setAutoscalingPolicyDefinition(withAlert(alert))
                                .setOriginalHostGroupNodeCount(clusterSize)
                                .setDesiredHostGroupNodeCount(clusterSize + adjustmentCount)
                                .build());

        usageReporter.cdpDatahubAutoscaleTriggered(cdpAutoscaleTriggerDetailsBuilder.build());
    }

    @Async
    public void reportAutoscalingConfigChanged(String userCrn, Cluster cluster) {
        UsageProto.CDPDatahubAutoscaleConfigChanged.Builder cdpAutoscaleConfigChangedBuilder = UsageProto.CDPDatahubAutoscaleConfigChanged.newBuilder();
        cdpAutoscaleConfigChangedBuilder.setUserCrn(userCrn);
        cdpAutoscaleConfigChangedBuilder.setAutoscalingEnabled(cluster.isAutoscalingEnabled());
        Stream.of(cluster.getLoadAlerts(), cluster.getTimeAlerts())
                .flatMap(Set::stream)
                .forEach(alert -> cdpAutoscaleConfigChangedBuilder.addAutoscalingPolicyDefinition(withAlert(alert)));

        usageReporter.cdpDatahubAutoscaleConfigChanged(cdpAutoscaleConfigChangedBuilder.build());
    }

    public UsageProto.CDPAutoscalePolicyDefinition withAlert(BaseAlert alert) {
        return UsageProto.CDPAutoscalePolicyDefinition.newBuilder()
                .setAutoscalingPolicyName(defaultIfEmpty(alert.getName(), ""))
                .setAutoscalingHostGroup(alert.getScalingPolicy().getHostGroup())
                .setAutoscalePolicyParameters(alert.getTelemetryParameters().toString())
                .setAutoscalePolicyType(getAutoscalePolicyType(alert.getAlertType()))
                .build();
    }

    private UsageProto.AutoscalePolicyType.Value getAutoscalePolicyType(AlertType alertType) {
        switch (alertType) {
            case LOAD:
                return UsageProto.AutoscalePolicyType.Value.LOAD_BASED;
            case TIME:
                return UsageProto.AutoscalePolicyType.Value.TIME_BASED;
            default:
                return UsageProto.AutoscalePolicyType.Value.UNSET;
        }
    }

    private UsageProto.AutoscaleScalingStatusType.Value getAutoscaleStatusType(ScalingStatus scalingStatus) {
        switch (scalingStatus) {
            case SUCCESS:
                return UsageProto.AutoscaleScalingStatusType.Value.SCALING_REQUESTED;
            case FAILED:
                return UsageProto.AutoscaleScalingStatusType.Value.SCALING_REQUEST_FAILED;
            case TRIGGER_FAILED:
                return UsageProto.AutoscaleScalingStatusType.Value.AUTOSCALE_TRIGGER_FAILED;
            default:
                return UsageProto.AutoscaleScalingStatusType.Value.UNSET;
        }
    }
}
