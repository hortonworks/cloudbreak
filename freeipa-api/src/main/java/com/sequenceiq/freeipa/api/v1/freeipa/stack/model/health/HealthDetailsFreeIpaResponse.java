package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.service.api.doc.ModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "HealthDetailsFreeIpaV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthDetailsFreeIpaResponse {
    @NotNull
    @Schema(description = ModelDescriptions.ENVIRONMENT_CRN, requiredMode = Schema.RequiredMode.REQUIRED)
    private String environmentCrn;

    @NotNull
    @Schema(description = FreeIpaModelDescriptions.FREEIPA_NAME, requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String crn;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<NodeHealthDetails> nodeHealthDetails = new ArrayList<>();

    private Status status;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<NodeHealthDetails> getNodeHealthDetails() {
        if (nodeHealthDetails == null) {
            nodeHealthDetails = new ArrayList<>();
        }
        return nodeHealthDetails;
    }

    public void setNodeHealthDetails(List<NodeHealthDetails> nodeHealthDetails) {
        this.nodeHealthDetails = nodeHealthDetails;
    }

    public void addNodeHealthDetailsFreeIpaResponses(NodeHealthDetails nodeHealthDetails) {
        if (this.nodeHealthDetails == null) {
            this.nodeHealthDetails = new ArrayList<>();
        }
        this.nodeHealthDetails.add(nodeHealthDetails);
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "HealthDetailsFreeIpaResponse{" +
                "environmentCrn='" + environmentCrn + '\'' +
                ", name='" + name + '\'' +
                ", crn='" + crn + '\'' +
                ", nodeHealthDetails=" + nodeHealthDetails +
                ", status=" + status +
                '}';
    }
}
