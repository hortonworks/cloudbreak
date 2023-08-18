package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseRequest {

    @ApiModelProperty(ModelDescriptions.CREATE_DATABASE_OPTION)
    private Boolean create;

    @ApiModelProperty(ModelDescriptions.DATABASE_AVAILABILITY_TYPE)
    private SdxDatabaseAvailabilityType availabilityType;

    @ApiModelProperty(ModelDescriptions.DATABASE_ENGINE_VERSION)
    @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version")
    private String databaseEngineVersion;

    @ApiModelProperty(ModelDescriptions.AZURE_DATABASE_REQUEST)
    @Deprecated
    private SdxDatabaseAzureRequest sdxDatabaseAzureRequest;

    public Boolean getCreate() {
        return create;
    }

    public void setCreate(Boolean create) {
        this.create = create;
    }

    public SdxDatabaseAvailabilityType getAvailabilityType() {
        return availabilityType;
    }

    public void setAvailabilityType(SdxDatabaseAvailabilityType availabilityType) {
        this.availabilityType = availabilityType;
    }

    public String getDatabaseEngineVersion() {
        return databaseEngineVersion;
    }

    public void setDatabaseEngineVersion(String databaseEngineVersion) {
        this.databaseEngineVersion = databaseEngineVersion;
    }

    @Deprecated
    public SdxDatabaseAzureRequest getSdxDatabaseAzureRequest() {
        return sdxDatabaseAzureRequest;
    }

    @Deprecated
    public void setSdxDatabaseAzureRequest(SdxDatabaseAzureRequest sdxDatabaseAzureRequest) {
        this.sdxDatabaseAzureRequest = sdxDatabaseAzureRequest;
    }
}
