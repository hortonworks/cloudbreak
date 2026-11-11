package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.DatabaseServerModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for changing the instance type of the database server of a cluster")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseServerChangeInstanceTypeV4Request {

    @NotEmpty
    @Schema(description = DatabaseServerModelDescription.INSTANCE_TYPE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String instanceType;

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    @Override
    public String toString() {
        return "DatabaseServerChangeInstanceTypeV4Request{" +
                "instanceType='" + instanceType + '\'' +
                '}';
    }
}
