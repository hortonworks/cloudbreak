package com.sequenceiq.periscope.monitor.evaluator.update;

import static com.google.common.collect.Sets.newConcurrentHashSet;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_FAILED;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_IN_PROGRESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.SCALING_FLOW_SUCCESS;
import static com.sequenceiq.periscope.api.model.ActivityStatus.UNKNOWN;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_SCALING_ACTIVITY_UNKNOWN;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_SCALING_FLOW_FAILED;
import static java.time.Instant.now;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.periscope.domain.ScalingActivity;
import com.sequenceiq.periscope.model.ScalingActivities;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.context.ScalingActivitiesEvaluatorContext;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;
import com.sequenceiq.periscope.monitor.handler.FlowCommunicator;
import com.sequenceiq.periscope.service.ScalingActivityService;

@Component("ActivityUpdateEvaluator")
@Scope("prototype")
public class ActivityUpdateEvaluator extends EvaluatorExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityUpdateEvaluator.class);

    private static final String EVALUATOR_NAME = ActivityUpdateEvaluator.class.getName();

    @Inject
    private FlowCommunicator flowCommunicator;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private ScalingActivityService scalingActivityService;

    private ScalingActivities scalingActivities;

    @Nonnull
    @Override
    public EvaluatorContext getContext() {
        return new ScalingActivitiesEvaluatorContext(scalingActivities);
    }

    @Override
    public String getName() {
        return EVALUATOR_NAME;
    }

    @Override
    public void setContext(EvaluatorContext context) {
        this.scalingActivities = (ScalingActivities) context.getData();
    }

    @Override
    protected void execute() {
        if (isEmpty(scalingActivities)) {
            LOGGER.info("No ScalingActivities found");
            return;
        }

        LOGGER.info("Executing ScalingActivityUpdateEvaluator: {}, for {} activityIds", scalingActivities.getId(), scalingActivities.getActivityIds().size());
        long start = now().toEpochMilli();
        try {
            if (scalingActivities.getActivityIds().isEmpty()) {
                LOGGER.info("No activityIds present, skipping request to update");
            } else {
                Map<String, Long> activityIdsByFlowIds = scalingActivityService.findAllByIds(scalingActivities.getActivityIds()).stream()
                        .collect(toMap(ScalingActivity::getFlowId, ScalingActivity::getId));
                Map<Long, FlowCheckResponse> flowResponseByActivityId = flowCommunicator.getFlowStatusFromFlowIds(activityIdsByFlowIds);

                Map<Long, Long> idsWithFailedFlows = new ConcurrentHashMap<>();
                Set<Long> idsWithFlowsInProgress = newConcurrentHashSet();
                Map<Long, Long> idsWithCompletedFlows = new ConcurrentHashMap<>();

                populateRespectiveActivityIdCollections(flowResponseByActivityId, idsWithFailedFlows, idsWithFlowsInProgress, idsWithCompletedFlows);

                List<Long> idsWithUnknownFlowDetails = activityIdsByFlowIds.values()
                        .stream()
                        .filter(v -> !flowResponseByActivityId.containsKey(v))
                        .collect(toList());

                scalingActivityService.setActivityStatusAndReasonForIds(idsWithFailedFlows.keySet(), SCALING_FLOW_FAILED,
                        messagesService.getMessage(AUTOSCALE_SCALING_FLOW_FAILED));
                scalingActivityService.setEndTimes(idsWithFailedFlows);

                scalingActivityService.setActivityStatusForIds(idsWithFlowsInProgress, SCALING_FLOW_IN_PROGRESS);

                scalingActivityService.setActivityStatusForIds(idsWithCompletedFlows.keySet(), SCALING_FLOW_SUCCESS);
                scalingActivityService.setEndTimes(idsWithCompletedFlows);

                scalingActivityService.setActivityStatusAndReasonForIds(idsWithUnknownFlowDetails, UNKNOWN,
                        messagesService.getMessage(AUTOSCALE_SCALING_ACTIVITY_UNKNOWN));
            }
        } catch (Exception e) {
            LOGGER.error("Encountered exception when executing ScalingActivityUpdateEvaluator: {}", scalingActivities.getId(), e);
        } finally {
            LOGGER.info("Finished executing ScalingActivityUpdateEvaluator in {}ms", now().toEpochMilli() - start);
        }
    }

    private void populateRespectiveActivityIdCollections(Map<Long, FlowCheckResponse> flowResponseByActivityId,
            Map<Long, Long> idsWithFailedFlows, Set<Long> idsWithFlowsInProgress, Map<Long, Long> idsWithCompletedFlows) {
        for (Entry<Long, FlowCheckResponse> e : flowResponseByActivityId.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue().getLatestFlowFinalizedAndFailed()) && !isNull(e.getValue().getEndTime())) {
                idsWithFailedFlows.put(e.getKey(), e.getValue().getEndTime());
            } else if (Boolean.TRUE.equals(e.getValue().getHasActiveFlow())) {
                idsWithFlowsInProgress.add(e.getKey());
            } else if (!Boolean.TRUE.equals(e.getValue().getLatestFlowFinalizedAndFailed()) && !Boolean.TRUE.equals(e.getValue().getHasActiveFlow())
                    && !isNull(e.getValue().getEndTime())) {
                idsWithCompletedFlows.put(e.getKey(), e.getValue().getEndTime());
            }
        }
    }
}
