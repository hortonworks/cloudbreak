package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.Map;
import java.util.Queue;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.repository.FlowChainLogRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;

@Service
@Transactional
public class FlowLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowLogService.class);

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FlowChainLogRepository flowChainLogRepository;

    @Inject
    @Qualifier("JsonWriterOptions")
    private Map<String, Object> writeOptions;

    public FlowLog save(String flowId, String flowChanId, String key, Payload payload, Map<Object, Object> variables, Class<?> flowType,
            FlowState currentState) {
        String payloadJson = JsonWriter.objectToJson(payload, writeOptions);
        String variablesJson = JsonWriter.objectToJson(variables, writeOptions);
        FlowLog flowLog = new FlowLog(payload.getStackId(), flowId, flowChanId, key, payloadJson, payload.getClass(), variablesJson, flowType,
                currentState.toString());
        flowLog.setCloudbreakNodeId(cloudbreakNodeConfig.getId());
        return flowLogRepository.save(flowLog);
    }

    public FlowLog close(Long stackId, String flowId) {
        return finalize(stackId, flowId, "FINISHED");
    }

    public FlowLog cancel(Long stackId, String flowId) {
        return finalize(stackId, flowId, "CANCELLED");
    }

    public FlowLog terminate(Long stackId, String flowId) {
        return finalize(stackId, flowId, "TERMINATED");
    }

    public void purgeTerminatedStacksFlowLogs() {
        LOGGER.info("Cleaning deleted stack's flowlog");
        int purgedTerminatedStackLogs = flowLogRepository.purgeTerminatedStackLogs();
        LOGGER.info("Deleted flowlog count: {}", purgedTerminatedStackLogs);
        LOGGER.info("Cleaning orphan flowchainlogs");
        int purgedOrphanFLowChainLogs = flowChainLogRepository.purgeOrphanFLowChainLogs();
        LOGGER.info("Deleted flowchainlog count: {}", purgedOrphanFLowChainLogs);
    }

    private FlowLog finalize(Long stackId, String flowId, String state) {
        flowLogRepository.finalizeByFlowId(flowId);
        FlowLog flowLog = new FlowLog(stackId, flowId, state, Boolean.TRUE);
        flowLog.setCloudbreakNodeId(cloudbreakNodeConfig.getId());
        return flowLogRepository.save(flowLog);
    }

    public FlowChainLog saveChain(String flowChainId, String parentFlowChainId, Queue<Selectable> chain) {
        String chainJson = JsonWriter.objectToJson(chain);
        FlowChainLog chainLog = new FlowChainLog(flowChainId, parentFlowChainId, chainJson);
        return flowChainLogRepository.save(chainLog);
    }
}
