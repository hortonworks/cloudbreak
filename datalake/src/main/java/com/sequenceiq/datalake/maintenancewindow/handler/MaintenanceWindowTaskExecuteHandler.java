package com.sequenceiq.datalake.maintenancewindow.handler;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

/**
 * Handles a single {@code task_type} dispatched from the maintenance window service to Data Lake.
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
