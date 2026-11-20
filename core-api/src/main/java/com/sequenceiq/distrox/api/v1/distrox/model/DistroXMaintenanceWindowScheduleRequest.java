package com.sequenceiq.distrox.api.v1.distrox.model;

import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleOnCreateRequest;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Optional Data Hub–scoped maintenance window override on create. Persisted by the maintenance service, not Core DB.
 */
@Schema(name = "DistroXMaintenanceWindowScheduleV1Request")
public class DistroXMaintenanceWindowScheduleRequest extends MaintenanceWindowScheduleOnCreateRequest {
}
