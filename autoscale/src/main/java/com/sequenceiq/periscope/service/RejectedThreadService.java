package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsStringSilent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.AutoscaleStackResponse;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Service
public class RejectedThreadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectedThreadService.class);

    private final Map<Long, RejectedThread> rejectedThreads = new HashMap<>();

    public RejectedThread save(RejectedThread rejectedThread) {
        rejectedThreads.put(rejectedThread.getId(), rejectedThread);
        return rejectedThread;
    }

    public List<RejectedThread> getAllRejectedCluster() {
        return new ArrayList<>(rejectedThreads.values());
    }

    public void create(EvaluatorExecutor task) {
        Object data = task.getContext().getData();
        RejectedThread rejectedThread;
        if (data instanceof AutoscaleStackResponse) {
            Long stackId = ((AutoscaleStackResponse) data).getStackId();
            rejectedThread = createOrUpdateRejectedThread(data, stackId);
        } else {
            rejectedThread = createOrUpdateRejectedThread(Collections.singletonMap("id", data), (Long) data);
        }

        LOGGER.info("Rejected task: {}, count: {}", rejectedThread.getJson(), rejectedThread.getRejectedCount());
        rejectedThread.setType(task.getClass().getName());
        save(rejectedThread);
    }

    private RejectedThread createOrUpdateRejectedThread(Object data, Long id) {
        RejectedThread rejectedThread = findById(id);
        if (rejectedThread == null) {
            rejectedThread = new RejectedThread();
            rejectedThread.setRejectedCount(1L);
            rejectedThread.setId(id);
            rejectedThread.setJson(writeValueAsStringSilent(data));
        } else {
            rejectedThread.setRejectedCount(rejectedThread.getRejectedCount() + 1L);
        }
        return rejectedThread;
    }

    public void remove(Object data) {
        RejectedThread removed;
        if (data instanceof AutoscaleStackResponse) {
            removed = rejectedThreads.remove(((AutoscaleStackResponse) data).getStackId());
        } else {
            removed = rejectedThreads.remove(data);
        }
        if (removed != null) {
            LOGGER.info("Rejected thread removed {} with count {}", removed.getJson(), removed.getRejectedCount());
        }
    }

    private RejectedThread findById(Long id) {
        return rejectedThreads.get(id);
    }
}
