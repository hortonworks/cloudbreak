package com.sequenceiq.cloudbreak.service.flowlog;

import java.util.Queue;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.domain.FlowChainLog;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.repository.FlowChainLogRepository;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;

@Service
@Transactional
public class FlowLogService {

    @Inject
    private FlowLogRepository flowLogRepository;

    @Inject
    private FlowChainLogRepository flowChainLogRepository;

    public FlowLog save(String flowId, String flowChanId, String key, Payload payload, Class<?> flowType, FlowState currentState) {
        String payloadJson = JsonWriter.objectToJson(payload);
        FlowLog flowLog = new FlowLog(payload.getStackId(), flowId, flowChanId, key, payloadJson, payload.getClass(), flowType, currentState.toString());
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

    private FlowLog finalize(Long stackId, String flowId, String state) {
        flowLogRepository.finalizeByFlowId(flowId);
        FlowLog flowLog = new FlowLog(stackId, flowId, state, Boolean.TRUE);
        return flowLogRepository.save(flowLog);
    }

    public FlowChainLog saveChain(String flowChainId, String parentFlowChainId, Queue<Selectable> chain) {
        String chainJson = JsonWriter.objectToJson(chain);
        FlowChainLog chainLog = new FlowChainLog(flowChainId, parentFlowChainId, chainJson);
        return flowChainLogRepository.save(chainLog);
    }
}
