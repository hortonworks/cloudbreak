package com.sequenceiq.maintenance.api.execution;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.maintenance.api.execution.model.MaintenanceTaskDispatchRequestBase;

/**
 * Handles a single {@code task_type} dispatched from the maintenance window service to a submitter
 * (Cloudbreak, Data Lake, ...). Each submitter implements it with its own dispatch request type.
 *
 * @param <R> submitter-specific dispatch request type
 */
public interface MaintenanceWindowTaskExecuteHandler<R extends MaintenanceTaskDispatchRequestBase> {

    /**
     * @return maintenance task type this handler supports (e.g. {@code SECRET_ROTATION})
     */
    String taskType();

    /**
     * Starts async work for the dispatch. Implementations return the started flow identifier when work was accepted,
     * or throw (e.g. {@link com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException}) when work cannot start.
     */
    FlowIdentifier execute(R request);
}
