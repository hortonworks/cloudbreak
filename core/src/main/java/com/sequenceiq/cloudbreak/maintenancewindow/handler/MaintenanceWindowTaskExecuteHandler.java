package com.sequenceiq.cloudbreak.maintenancewindow.handler;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;

/**
 * Handles a single {@code task_type} dispatched from the maintenance window service to Cloudbreak.
 */
public interface MaintenanceWindowTaskExecuteHandler {

    /**
     * @return maintenance task type this handler supports (e.g. {@code SECRET_ROTATION})
     */
    String taskType();

    /**
     * Starts async work for the dispatch. Implementations return the started flow identifier when work was accepted,
     * or throw (e.g. {@link com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException}) when work cannot start.
     */
    FlowIdentifier execute(MaintenanceTaskDispatchRequest request);
}
