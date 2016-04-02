package com.sequenceiq.cloudbreak.service.flowlog;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cedarsoftware.util.io.JsonWriter;
import com.sequenceiq.cloudbreak.core.flow2.FlowState;
import com.sequenceiq.cloudbreak.domain.FlowLog;
import com.sequenceiq.cloudbreak.repository.FlowLogRepository;

@Service
public class FlowLogService {

    @Inject
    private FlowLogRepository flowLogRepository;

    public FlowLog save(String flowId, String key, Object payload, Class<?> flowType, FlowState<?, ?> currentState) {
        String payloadJson = JsonWriter.objectToJson(payload);
        FlowLog flowLog = new FlowLog(flowId, key, payloadJson, payload.getClass(), flowType, currentState.toString());
        return flowLogRepository.save(flowLog);
    }

    public FlowLog close(String flowId) {
        FlowLog flowLog = new FlowLog(flowId, "FINISHED");
        return flowLogRepository.save(flowLog);
    }
}
