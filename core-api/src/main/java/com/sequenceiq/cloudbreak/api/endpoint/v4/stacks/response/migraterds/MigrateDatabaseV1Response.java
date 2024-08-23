package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.migraterds;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.MigrateDatabaseResponseType;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.MigrateRdsDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MigrateDatabaseV1Response {

    @Schema(description = MigrateRdsDescription.FLOW_ID)
    private FlowIdentifier flowIdentifier;

    @Schema(description = MigrateRdsDescription.MIGRATE_DATABASE_TO_SSL_RESPONSE_TYPE)
    private MigrateDatabaseResponseType responseType;

    @Schema(description = MigrateRdsDescription.MIGRATE_DATABASE_TO_SSL_ERROR_REASON)
    private String reason;

    @Schema(description = StackModelDescription.CRN)
    private String resourceCrn;

    public MigrateDatabaseV1Response() {
    }

    public MigrateDatabaseV1Response(MigrateDatabaseResponseType responseType, FlowIdentifier flowIdentifier, String reason, String resourceCrn) {
        this.responseType = responseType;
        this.flowIdentifier = flowIdentifier;
        this.reason = reason;
        this.resourceCrn = resourceCrn;
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public MigrateDatabaseResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(MigrateDatabaseResponseType responseType) {
        this.responseType = responseType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        MigrateDatabaseV1Response that = (MigrateDatabaseV1Response) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType &&
                Objects.equals(reason, that.reason) && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "MigrateDatabaseV1Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
