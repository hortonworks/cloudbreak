package com.sequenceiq.sdx.api.model.migraterds;

import static com.sequenceiq.sdx.api.model.ModelDescriptions.DATA_LAKE_CRN;
import static com.sequenceiq.sdx.api.model.ModelDescriptions.SdxRotateRdsCertificateDescription.FLOW_ID;
import static com.sequenceiq.sdx.api.model.ModelDescriptions.SdxRotateRdsCertificateDescription.MIGRATE_DATABASE_TO_SSL_ERROR_REASON;
import static com.sequenceiq.sdx.api.model.ModelDescriptions.SdxRotateRdsCertificateDescription.MIGRATE_DATABASE_TO_SSL_RESPONSE_TYPE;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxMigrateDatabaseV1Response {

    @Schema(description = FLOW_ID)
    private FlowIdentifier flowIdentifier;

    @Schema(description = MIGRATE_DATABASE_TO_SSL_RESPONSE_TYPE)
    private SdxMigrateDatabaseResponseType responseType;

    @Schema(description = MIGRATE_DATABASE_TO_SSL_ERROR_REASON)
    private String reason;

    @Schema(description = DATA_LAKE_CRN)
    private String resourceCrn;

    public SdxMigrateDatabaseV1Response() {
    }

    public SdxMigrateDatabaseV1Response(SdxMigrateDatabaseResponseType responseType, FlowIdentifier flowIdentifier, String reason, String resourceCrn) {
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

    public SdxMigrateDatabaseResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(SdxMigrateDatabaseResponseType responseType) {
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
        SdxMigrateDatabaseV1Response that = (SdxMigrateDatabaseV1Response) o;
        return Objects.equals(flowIdentifier, that.flowIdentifier) && responseType == that.responseType &&
                Objects.equals(reason, that.reason) && Objects.equals(resourceCrn, that.resourceCrn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowIdentifier, responseType, reason, resourceCrn);
    }

    @Override
    public String toString() {
        return "SdxMigrateDatabaseV1Response{" +
                "flowIdentifier=" + flowIdentifier +
                ", responseType=" + responseType +
                ", reason='" + reason + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}
