package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ACTIVITY_NODE_LIMIT_EXCEEDED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ACTIVITY_SUCCESS;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.periscope.api.model.ScalingStatus;
import com.sequenceiq.periscope.common.MessageCode;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.domain.MetricType;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.notification.HttpNotificationSender;
import com.sequenceiq.periscope.service.AuditService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.PeriscopeMetricService;
import com.sequenceiq.periscope.service.UsageReportingService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;
import com.sequenceiq.periscope.utils.LoggingUtils;

@Component("ScalingRequest")
@Scope("prototype")
public class ScalingRequest implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingRequest.class);

    private static final int STATUSREASON_MAX_LENGTH = 255;

    private final int existingClusterNodeCount;

    private final int desiredHostGroupNodeCount;

    private final int existingHostGroupNodeCount;

    private final Cluster cluster;

    private final ScalingPolicy policy;

    private List<String> decommissionNodeIds;

    @Inject
    private CloudbreakInternalCrnClient cloudbreakCrnClient;

    @Inject
    private HistoryService historyService;

    @Inject
    private HttpNotificationSender notificationSender;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private PeriscopeMetricService metricService;

    @Inject
    private CloudbreakCommunicator cloudbreakCommunicator;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private AuditService auditService;

    @Inject
    private UsageReportingService usageReportingService;

    @Inject
    private LimitsConfigurationService limitsConfigurationService;

    public ScalingRequest(Cluster cluster, ScalingPolicy policy, int existingClusterNodeCount, int existingHostGroupNodeCount,
            int desiredHostGroupNodeCount, List<String> decommissionNodeIds) {
        this.cluster = cluster;
        this.policy = policy;
        this.existingClusterNodeCount = existingClusterNodeCount;
        this.existingHostGroupNodeCount = existingHostGroupNodeCount;
        this.desiredHostGroupNodeCount = desiredHostGroupNodeCount;
        this.decommissionNodeIds = decommissionNodeIds;
    }

    @Override
    public void run() {
        LoggingUtils.buildMdcContext(cluster);
        try {
            int scalingAdjustment = desiredHostGroupNodeCount - existingHostGroupNodeCount;
            if (!decommissionNodeIds.isEmpty()) {
                scaleDownByNodeIds(decommissionNodeIds);
            } else if (scalingAdjustment > 0) {
                Integer cbSupportedScalingAdjustment = getScaleUpNodeCountForCBSupportedMax(scalingAdjustment);
                if (cbSupportedScalingAdjustment > 0) {
                    scaleUp(cbSupportedScalingAdjustment, existingHostGroupNodeCount);
                }
            } else if (scalingAdjustment < 0) {
                scaleDown(scalingAdjustment, existingHostGroupNodeCount);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Error while executing ScaleRequest", e);
        }
    }

    private void scaleUp(int scalingAdjustment, int hostGroupNodeCount) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_TRIGGERED);
        if (scalingHardLimitsService.isViolatingAutoscaleMaxStepInNodeCount(scalingAdjustment)) {
            LOGGER.info("Upscale requested for '{}' nodes. Upscaling with the maximum allowed step size of '{}' node(s)",
                    scalingAdjustment, scalingHardLimitsService.getMaxAutoscaleStepInNodeCount());
            scalingAdjustment = scalingHardLimitsService.getMaxAutoscaleStepInNodeCount();
        }
        String hostGroup = policy.getHostGroup();
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        String stackCrn = cluster.getStackCrn();
        String userCrn = cluster.getClusterPertain().getUserCrn();
        try {

            LOGGER.info("Sending request to add '{}' instance(s) into host group '{}', triggered adjustmentType '{}', cluster '{}', user '{}'",
                    scalingAdjustment, hostGroup, policy.getAdjustmentType(), stackCrn, userCrn);
            UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
            updateStackJson.setWithClusterEvent(true);
            InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            instanceGroupAdjustmentJson.setInstanceGroup(hostGroup);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);

            cloudbreakCrnClient.withInternalCrn().autoscaleEndpoint().putStack(stackCrn, cluster.getClusterPertain().getUserId(), updateStackJson);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason = getMessageForCBSuccess();
            metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL);
        } catch (RuntimeException e) {
            scalingStatus = ScalingStatus.FAILED;
            statusReason = getMessageForCBException(e);
            LOGGER.error("Couldn't trigger upscaling for host group '{}', cluster '{}', desiredNodeCount '{}', error '{}' ",
                    hostGroup, cluster.getStackCrn(), desiredHostGroupNodeCount, statusReason, e);
            metricService.incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED);
        } finally {
            processAutoscalingTriggered(scalingAdjustment, hostGroupNodeCount, statusReason, scalingStatus);
        }
    }

    private void scaleDown(int scalingAdjustment, int totalNodes) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_TRIGGERED);
        String hostGroup = policy.getHostGroup();
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        String stackCrn = cluster.getStackCrn();
        String userCrn = cluster.getClusterPertain().getUserCrn();
        try {
            LOGGER.info("Sending request to remove '{}' node(s) from host group '{}', triggered adjustmentType '{}', cluster '{}', user '{}'",
                    scalingAdjustment, hostGroup, policy.getAdjustmentType(), stackCrn, userCrn);
            UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
            HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            hostGroupAdjustmentJson.setWithStackUpdate(true);
            hostGroupAdjustmentJson.setHostGroup(hostGroup);
            hostGroupAdjustmentJson.setValidateNodeCount(false);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            cloudbreakCrnClient.withInternalCrn().autoscaleEndpoint()
                    .putCluster(stackCrn, cluster.getClusterPertain().getUserId(), updateClusterJson);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason = getMessageForCBSuccess();
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_SUCCESSFUL);
        } catch (Exception e) {
            scalingStatus = ScalingStatus.FAILED;
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_FAILED);
            statusReason = getMessageForCBException(e);
            LOGGER.error("Couldn't trigger downscaling for host group '{}', cluster '{}', desiredNodeCount '{}', error '{}' ",
                    hostGroup, cluster.getStackCrn(), desiredHostGroupNodeCount, statusReason, e);
        } finally {
            processAutoscalingTriggered(scalingAdjustment, totalNodes, statusReason, scalingStatus);
        }
    }

    private void scaleDownByNodeIds(List<String> decommissionNodeIds) {
        metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_TRIGGERED);
        String hostGroup = policy.getHostGroup();
        String statusReason = null;
        ScalingStatus scalingStatus = null;
        try {
            LOGGER.info("Sending request to remove  nodeIdCount '{}', nodeId(s) '{}' from host group '{}', cluster '{}', user '{}'",
                    decommissionNodeIds.size(), decommissionNodeIds, hostGroup, cluster.getStackCrn(),
                    cluster.getClusterPertain().getUserCrn());
            cloudbreakCommunicator.decommissionInstancesForCluster(cluster, decommissionNodeIds);
            scalingStatus = ScalingStatus.SUCCESS;
            statusReason = getMessageForCBSuccess();
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_SUCCESSFUL);
        } catch (Exception e) {
            scalingStatus = ScalingStatus.FAILED;
            metricService.incrementMetricCounter(MetricType.CLUSTER_DOWNSCALE_FAILED);
            statusReason = getMessageForCBException(e);
            LOGGER.error("Couldn't trigger decommissioning for host group '{}', cluster '{}', decommissionNodeCount '{}', " +
                            "decommissionNodeIds '{}', error '{}' ", hostGroup, cluster.getStackCrn(), decommissionNodeIds.size(),
                    decommissionNodeIds, statusReason, e);
        } finally {
            processAutoscalingTriggered(-decommissionNodeIds.size(), existingHostGroupNodeCount, statusReason, scalingStatus);
        }
    }

    private Integer getScaleUpNodeCountForCBSupportedMax(Integer scalingAdjustment) {
        int cbSupportedMaxClusterSize = limitsConfigurationService.getMaxNodeCountLimit();
        if ((existingClusterNodeCount + scalingAdjustment) > cbSupportedMaxClusterSize) {
            int requestedScalingAdjustment = scalingAdjustment;
            scalingAdjustment = cbSupportedMaxClusterSize - existingClusterNodeCount;

            String nodeLimitExceededMsg = messagesService.getMessage(AUTOSCALING_ACTIVITY_NODE_LIMIT_EXCEEDED,
                    List.of(policy.getAlert().getAlertType(), requestedScalingAdjustment, cbSupportedMaxClusterSize, scalingAdjustment));
            historyService.createEntry(ScalingStatus.TRIGGER_INFO, nodeLimitExceededMsg, cluster);
            LOGGER.info(nodeLimitExceededMsg + " Existing Cluster Size '" + existingClusterNodeCount + "'.");
        }
        return scalingAdjustment;
    }

    private void processAutoscalingTriggered(int adjustmentCount, int hostGroupNodeCount, String statusReason, ScalingStatus scalingStatus) {
        History history = historyService.createEntry(scalingStatus,
                StringUtils.left(statusReason, STATUSREASON_MAX_LENGTH), hostGroupNodeCount, adjustmentCount, policy);
        notificationSender.sendHistoryUpdateNotification(history, cluster);
        auditService.auditAutoscaleServiceEvent(scalingStatus, statusReason, cluster.getStackCrn(),
                cluster.getClusterPertain().getTenant(), System.currentTimeMillis());
        usageReportingService.reportAutoscalingTriggered(adjustmentCount, hostGroupNodeCount, scalingStatus, statusReason, policy.getAlert(), cluster);
    }

    private String getMessageForCBException(Exception cbApiException) {
        String cbApiError = cbApiException.getMessage();
        if (cbApiException instanceof ClientErrorException) {
            try (Response exceptionResponse = ((ClientErrorException) cbApiException).getResponse()) {
                cbApiError = exceptionResponse.readEntity(ExceptionResponse.class).getMessage();
            } catch (Exception ex) {
                LOGGER.error("Error processing CB API Exception '{}'", cbApiException, ex);
            }
        }
        return messagesService.getMessage(MessageCode.CLUSTER_SCALING_FAILED,
                List.of(policy.getAlert().getAlertType(), Optional.ofNullable(cbApiError).orElse("")));
    }

    private String getMessageForCBSuccess() {
        return messagesService.getMessage(AUTOSCALING_ACTIVITY_SUCCESS,
                List.of(policy.getAlert().getAlertType(), policy.getHostGroup(), existingHostGroupNodeCount, desiredHostGroupNodeCount));
    }

    @VisibleForTesting
    void setMetricService(PeriscopeMetricService metricService) {
        this.metricService = metricService;
    }

    @VisibleForTesting
    void setScalingHardLimitsService(ScalingHardLimitsService scalingHardLimitsService) {
        this.scalingHardLimitsService = scalingHardLimitsService;
    }

    @VisibleForTesting
    void setLimitsConfigurationService(LimitsConfigurationService limitsConfigurationService) {
        this.limitsConfigurationService = limitsConfigurationService;
    }

    @VisibleForTesting
    void setCloudbreakInternalCrnClient(CloudbreakInternalCrnClient cloudbreakCrnClient) {
        this.cloudbreakCrnClient = cloudbreakCrnClient;
    }

    @VisibleForTesting
    void setCloudbreakMessagesService(CloudbreakMessagesService messagesService) {
        this.messagesService = messagesService;
    }

    @VisibleForTesting
    void setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
    }

    @VisibleForTesting
    void setHttpNotificationSender(HttpNotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    @VisibleForTesting
    void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    @VisibleForTesting
    void setUsageReportingService(UsageReportingService usageReportingService) {
        this.usageReportingService = usageReportingService;
    }
}
