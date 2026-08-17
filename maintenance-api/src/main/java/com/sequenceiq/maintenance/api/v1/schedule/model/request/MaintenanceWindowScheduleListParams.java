package com.sequenceiq.maintenance.api.v1.schedule.model.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.ws.rs.QueryParam;

import com.sequenceiq.cloudbreak.util.OneOfEnum;
import com.sequenceiq.maintenance.api.doc.ModelDescriptions;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = ModelDescriptions.ScheduleListParams.PARAMS)
public class MaintenanceWindowScheduleListParams {

    @QueryParam("scopeType")
    @OneOfEnum(enumClass = MaintenanceScopeType.class, message = "Value must be one of the followings %s", fieldName = "scopeType")
    @Schema(description = ModelDescriptions.ScheduleListParams.SCOPE_TYPE)
    private String scopeType;

    @QueryParam("scopeId")
    @Schema(description = ModelDescriptions.ScheduleListParams.SCOPE_ID)
    private String scopeId;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @AssertTrue(message = "scopeType and scopeId must both be provided or both omitted")
    public boolean isScopeFilterValid() {
        boolean hasScopeType = scopeType != null && !scopeType.isBlank();
        boolean hasScopeId = scopeId != null && !scopeId.isBlank();
        return hasScopeType == hasScopeId;
    }
}
