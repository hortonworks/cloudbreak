package com.sequenceiq.maintenance.dispatcher;

import com.sequenceiq.maintenance.dispatcher.model.MaintenanceTaskSubmitterDispatchResult;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

public interface MaintenanceTaskSubmitterClient {

    MaintenanceTaskSubmitterDispatchResult invokeExecuteCallback(
            MaintenanceWindowTask task,
            MaintenanceWindowRun run,
            MaintenanceWindowSchedule schedule,
            WindowOccurrence occurrence,
            String policyRevision);
}
