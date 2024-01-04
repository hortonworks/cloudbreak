package com.sequenceiq.cloudbreak.conclusion.step;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.TypedJsonUtil;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Component
public class InfoCollectorConclusionStep extends ConclusionStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoCollectorConclusionStep.class);

    @Inject
    private FlowLogDBService flowLogDBService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public Conclusion check(Long resourceId) {
        try {
            collectDebugInfo(resourceId);
        } catch (Exception e) {
            LOGGER.warn("Collecting debug info failed", e);
        }
        return succeeded();
    }

    private void collectDebugInfo(Long resourceId) {
        StringBuilder debugInfo = new StringBuilder("Flow failed for stackId: ").append(resourceId).append(".\n");
        List<FlowLog> flowLogs = flowLogDBService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(resourceId);
        if (!flowLogs.isEmpty()) {
            FlowLog latestFlowLog = flowLogs.iterator().next();
            appendFlowInfo(debugInfo, flowLogs, latestFlowLog);
            String flowChainId = latestFlowLog.getFlowChainId();
            FlowLog firstFlowLog = flowLogs.get(flowLogs.size() - 1);
            Long flowStartedTimeStamp = firstFlowLog.getCreated();
            if (StringUtils.isNotBlank(flowChainId)) {
                List<FlowChainLog> flowChainLogs = flowChainLogService.findByFlowChainIdOrderByCreatedDesc(flowChainId);
                if (!flowChainLogs.isEmpty()) {
                    List<FlowChainLog> relatedFlowChainLogs = flowChainLogService.getRelatedFlowChainLogs(flowChainLogs);
                    if (relatedFlowChainLogs.size() > 1) {
                        debugInfo.append("\nRelated flowChains: \n");
                        relatedFlowChainLogs.stream().forEach(relatedFlowChainLog -> appendRelatedFlowChainInfo(debugInfo, relatedFlowChainLog));
                    }
                    FlowChainLog firstFlowChainLog = relatedFlowChainLogs.iterator().next();
                    flowStartedTimeStamp = firstFlowChainLog.getCreated();
                    FlowChainLog latestFlowChainFirstLog = flowChainLogs.get(flowChainLogs.size() - 1);
                    appendLatestFlowChainInfo(debugInfo, latestFlowChainFirstLog);
                }
            }

            List<StackStatus> stackStatuses = stackStatusService.findAllStackStatusesById(resourceId, flowStartedTimeStamp);
            appendStackStatusInfo(debugInfo, stackStatuses);

            List<InstanceMetadataView> instanceMetadatas = instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(resourceId);
            appendInstanceMetadatasInfo(debugInfo, instanceMetadatas);
            LOGGER.info("Collected debug info: {}", debugInfo);
        } else {
            LOGGER.warn("No active flow found for stackId: {}", resourceId);
        }
    }

    private void appendInstanceMetadatasInfo(StringBuilder debugInfo, List<InstanceMetadataView> instanceMetadatas) {
        Multimap<String, InstanceMetadataView> instanceMetadatasByInstanceGroup = Multimaps.index(instanceMetadatas,
                InstanceMetadataView::getInstanceGroupName);
        debugInfo.append("\nInstance metadatas: \n");
        instanceMetadatasByInstanceGroup.asMap().entrySet().forEach(instanceMetadataGroupEntry -> {
            debugInfo.append(" - Instance group: ").append(instanceMetadataGroupEntry.getKey()).append("\n");
            instanceMetadataGroupEntry.getValue().stream().forEach(instanceMetadata -> appendInstanceMetadataInfo(debugInfo, instanceMetadata));
        });
    }

    private void appendInstanceMetadataInfo(StringBuilder debugInfo, InstanceMetadataView instanceMetadata) {
        debugInfo.append("   -- instanceId: ").append(instanceMetadata.getInstanceId())
                .append(", privateId: ").append(instanceMetadata.getPrivateId())
                .append(", discoveryFQDN: ").append(instanceMetadata.getDiscoveryFQDN())
                .append(", instanceStatus: ").append(instanceMetadata.getInstanceStatus())
                .append(", statusReason: ").append(instanceMetadata.getStatusReason())
                .append("\n");
    }

    private void appendRelatedFlowChainInfo(StringBuilder debugInfo, FlowChainLog relatedFlowChainLog) {
        debugInfo.append(" - Related flowChainId: ").append(relatedFlowChainLog.getFlowChainId())
                .append(", parentFlowChainId: ").append(relatedFlowChainLog.getParentFlowChainId())
                .append(", flowChainType: ").append(relatedFlowChainLog.getFlowChainType())
                .append(", triggerEvent: ").append(relatedFlowChainLog.getTriggerEventJackson())
                .append("\n");
    }

    private void appendStackStatusInfo(StringBuilder debugInfo, List<StackStatus> latestStackStatuses) {
        debugInfo.append("\nPrevious stack statuses: \n");
        latestStackStatuses.stream().forEach(stackStatus -> appendStackStatusInfo(debugInfo, stackStatus));
    }

    private void appendLatestFlowChainInfo(StringBuilder debugInfo, FlowChainLog flowChainLog) {
        debugInfo.append("\nLatest flowChainId: ").append(flowChainLog.getFlowChainId())
                .append(", parentFlowChainId: ").append(flowChainLog.getParentFlowChainId())
                .append(", flowChainType: ").append(flowChainLog.getFlowChainType())
                .append(", triggerEvent: ").append(flowChainLog.getTriggerEventJackson())
                .append("\n");
        debugInfo.append("Flow chain steps: \n");

        try {
            Queue<Selectable> queue = flowChainLog.getChainJackson() == null
                    ? null
                    : TypedJsonUtil.readValue(flowChainLog.getChainJackson(), Queue.class);
            if (queue != null) {
                queue.forEach(selectable -> appentFlowChainElementInfo(debugInfo, selectable));
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse flow chain queue", e);
        }
    }

    private void appendFlowInfo(StringBuilder debugInfo, List<FlowLog> flowLogs, FlowLog latestFlowLog) {
        debugInfo.append("Failed flowId: ").append(latestFlowLog.getFlowId())
                .append(", flowType: ").append(latestFlowLog.getFlowType().getName())
                .append(".\n");
        debugInfo.append("Previous flow steps: \n");
        flowLogs.stream().forEach(flowLog -> appendFlowLogInfo(debugInfo, flowLog));
    }

    private void appendStackStatusInfo(StringBuilder debugInfo, StackStatus stackStatus) {
        debugInfo.append(" - detailedStackStatus: ").append(stackStatus.getDetailedStackStatus())
                .append(", status: ").append(stackStatus.getStatus())
                .append(", statusReason: ").append(stackStatus.getStatusReason())
                .append(", created: ").append(convertToZonedDate(stackStatus.getCreated()))
                .append("\n");
    }

    private void appentFlowChainElementInfo(StringBuilder debugInfo, Selectable selectable) {
        debugInfo.append(" - selector: ").append(selectable.selector())
                .append(", event: ").append(selectable)
                .append("\n");
    }

    private void appendFlowLogInfo(StringBuilder debugInfo, FlowLog flowLog) {
        debugInfo.append(" - currentState: ").append(flowLog.getCurrentState())
                .append(", stateStatus: ").append(flowLog.getStateStatus())
                .append(", nextEvent: ").append(flowLog.getNextEvent())
                .append(", created: ").append(convertToZonedDate(flowLog.getCreated()))
                .append("\n");
    }

    private String convertToZonedDate(Long timestamp) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }
}
