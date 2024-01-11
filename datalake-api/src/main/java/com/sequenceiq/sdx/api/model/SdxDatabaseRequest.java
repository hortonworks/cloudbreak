package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.cloudbreak.common.database.DatabaseCommon.POSTGRES_VERSION_REGEX;

import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SdxDatabaseRequest {

    @Schema(description = ModelDescriptions.CREATE_DATABASE_OPTION)
    private Boolean create;

    @Schema(description = ModelDescriptions.DATABASE_AVAILABILITY_TYPE)
    private SdxDatabaseAvailabilityType availabilityType;

    @Pattern(regexp = POSTGRES_VERSION_REGEX, message = "Not a valid database major version")
    @Schema(description = ModelDescriptions.DATABASE_ENGINE_VERSION)
    private String databaseEngineVersion;

    @Schema(description = ModelDescriptions.AZURE_DATABASE_REQUEST)
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

    public SdxDatabaseAzureRequest getSdxDatabaseAzureRequest() {
        return sdxDatabaseAzureRequest;
    }

    public void setSdxDatabaseAzureRequest(SdxDatabaseAzureRequest sdxDatabaseAzureRequest) {
        this.sdxDatabaseAzureRequest = sdxDatabaseAzureRequest;
    }

    @Override
    public String toString() {
        return "SdxDatabaseRequest{" +
                "create=" + create +
                ", availabilityType=" + availabilityType +
                ", databaseEngineVersion='" + databaseEngineVersion + '\'' +
                ", sdxDatabaseAzureRequest=" + sdxDatabaseAzureRequest +
                '}';
    }
}
