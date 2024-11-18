package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import static com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor.VM_DATALAKE;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidCrn;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalDatabaseManageDatabaseUserV4Request {

    @Schema(description = ModelDescriptions.StackModelDescription.CRN)
    @ValidCrn(resource = VM_DATALAKE)
    private String crn;

    @Schema(description = ModelDescriptions.Database.USER_OPERATION)
    @NotEmpty
    private String operation;

    @Schema(description = ModelDescriptions.Database.RDSTYPE)
    @NotEmpty
    private String dbType;

    @Schema(description = ModelDescriptions.Database.USERNAME)
    @NotEmpty
    private String dbUser;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getDbUser() {
        return dbUser;
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
    }
}
