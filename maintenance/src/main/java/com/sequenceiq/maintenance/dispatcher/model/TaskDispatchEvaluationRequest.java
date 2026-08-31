package com.sequenceiq.maintenance.dispatcher.model;

import java.util.List;

import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

/**
 * Input to {@link com.sequenceiq.maintenance.dispatcher.MaintenanceWindowTaskDispatchEvaluator#evaluate}.
 *
 * @param task         the ACTIVE task being evaluated
 * @param occurrence   the current maintenance window occurrence
 * @param activeTasks  authoritative snapshot of every {@link com.sequenceiq.maintenance.domain.MaintenanceTaskStatus#ACTIVE}
 *                     task loaded once for the dispatcher tick (typically
 *                     {@code findByStatusOrderByPriorityDescCreatedAtAsc(ACTIVE)}); used to resolve {@code depends_on}
 *                     by id without a database round trip and to evaluate implicit tier ordering among peers in
 *                     {@code task}'s environment
 */
public record TaskDispatchEvaluationRequest(
        MaintenanceWindowTask task,
        WindowOccurrence occurrence,
        List<MaintenanceWindowTask> activeTasks) {
}
