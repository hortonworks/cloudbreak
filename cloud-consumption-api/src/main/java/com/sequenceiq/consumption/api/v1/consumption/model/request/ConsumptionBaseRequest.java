package com.sequenceiq.consumption.api.v1.consumption.model.request;

import java.io.Serializable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.consumption.api.doc.ConsumptionModelDescription;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(subTypes = StorageConsumptionRequest.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ConsumptionBaseRequest implements Serializable {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotNull
    @Schema(description = ConsumptionModelDescription.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @NotNull
    @Schema(description = ConsumptionModelDescription.MONITORED_RESOURCE_TYPE, required = true)
    private ResourceType monitoredResourceType;

    @ValidCrn(resource = {CrnResourceDescriptor.ENVIRONMENT, CrnResourceDescriptor.DATALAKE, CrnResourceDescriptor.DATAHUB})
    @NotNull
    @Schema(description = ConsumptionModelDescription.MONITORED_RESOURCE_CRN, required = true)
    private String monitoredResourceCrn;

    @NotEmpty
    @Schema(description = ConsumptionModelDescription.MONITORED_RESOURCE_NAME, required = true)
    private String monitoredResourceName;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public ResourceType getMonitoredResourceType() {
        return monitoredResourceType;
    }

    public void setMonitoredResourceType(ResourceType monitoredResourceType) {
        this.monitoredResourceType = monitoredResourceType;
    }

    public String getMonitoredResourceCrn() {
        return monitoredResourceCrn;
    }

    public void setMonitoredResourceCrn(String monitoredResourceCrn) {
        this.monitoredResourceCrn = monitoredResourceCrn;
    }

    public String getMonitoredResourceName() {
        return monitoredResourceName;
    }

    public void setMonitoredResourceName(String monitoredResourceName) {
        this.monitoredResourceName = monitoredResourceName;
    }

    @Override
    public String toString() {
        return "ConsumptionBaseRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", monitoredResourceType='" + monitoredResourceType + '\'' +
                ", monitoredResourceCrn='" + monitoredResourceCrn + '\'' +
                ", monitoredResourceName='" + monitoredResourceName + '\'' +
                '}';
    }

}
