package com.sequenceiq.periscope.service;

import static com.sequenceiq.cloudbreak.common.json.JsonUtil.writeValueAsStringSilent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.AutoscaleStackV4Response;
import com.sequenceiq.periscope.model.RejectedThread;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Service
public class RejectedThreadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RejectedThreadService.class);

    private final Map<Long, RejectedThread> rejectedThreads = new ConcurrentHashMap<>();

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
        if (data instanceof AutoscaleStackV4Response) {
            Long stackId = ((AutoscaleStackV4Response) data).getStackId();
            rejectedThread = createOrUpdateRejectedThread(data, stackId);
        } else if (data instanceof Long) {
            rejectedThread = createOrUpdateRejectedThread(Collections.singletonMap("id", data), (Long) data);
        } else if (data instanceof ScalingActivities) {
            rejectedThread = createOrUpdateRejectedThread(data, ((ScalingActivities) data).getId());
        } else {
            throw new IllegalArgumentException("The given data does not match AutoscaleStackResponse, Long or ScalingActivities, not possible to create");
        }

        LOGGER.debug("Rejected task: {}, count: {}", rejectedThread.getJson(), rejectedThread.getRejectedCount());
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
        if (data instanceof AutoscaleStackV4Response) {
            removed = rejectedThreads.remove(((AutoscaleStackV4Response) data).getStackId());
        } else if (data instanceof ScalingActivities) {
            removed = rejectedThreads.remove(((ScalingActivities) data).getId());
        } else if (data instanceof Long) {
            removed = rejectedThreads.remove(data);
        } else {
            throw new IllegalArgumentException("The given data does not match AutoscaleStackResponse, Long or ScalingActivities so not removable");
        }
        if (removed != null) {
            LOGGER.debug("Rejected thread removed {} with count {}", removed.getJson(), removed.getRejectedCount());
        }
    }

    private RejectedThread findById(Long id) {
        return rejectedThreads.get(id);
    }
}
