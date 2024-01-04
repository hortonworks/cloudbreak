package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotEmpty;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RepairInstancesV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepairInstancesRequest {

    @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT)
    @NotEmpty
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, required = true)
    private String environmentCrn;

    @Schema(description = ModelDescriptions.FORCE_REPAIR)
    private boolean forceRepair;

    @Schema(description = ModelDescriptions.INSTANCE_ID)
    private List<String> instanceIds;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public boolean isForceRepair() {
        return forceRepair;
    }

    public void setForceRepair(boolean forceRepair) {
        this.forceRepair = forceRepair;
    }

    public List<String> getInstanceIds() {
        return instanceIds == null ? instanceIds : instanceIds.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    public void setInstanceIds(List<String> instanceIds) {
        this.instanceIds = instanceIds;
    }

    @Override
    public String toString() {
        return "RepairInstancesRequest{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", forceRepair=" + forceRepair +
                ", instanceIds=" + instanceIds +
                '}';
    }
}
