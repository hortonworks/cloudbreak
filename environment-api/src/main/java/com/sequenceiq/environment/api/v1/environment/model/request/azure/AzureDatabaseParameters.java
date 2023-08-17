package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "AzureDatabaseV1Parameters")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureDatabaseParameters implements Serializable {

    @ApiModelProperty(EnvironmentModelDescription.AZURE_DATABASE_SETUP)
    private DatabaseSetup databaseSetup;

    public DatabaseSetup getDatabaseSetup() {
        return databaseSetup;
    }

    public void setDatabaseSetup(DatabaseSetup databaseSetup) {
        this.databaseSetup = databaseSetup;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureDatabaseParameters{" +
                "databaseSetup=" + databaseSetup +
                '}';
    }

    public static class Builder {

        private DatabaseSetup databaseSetup;

        private Builder() {
        }

        public Builder withDatabaseSetup(DatabaseSetup databaseSetup) {
            this.databaseSetup = databaseSetup;
            return this;
        }

        public AzureDatabaseParameters build() {
            AzureDatabaseParameters databaseParameters = new AzureDatabaseParameters();
            databaseParameters.setDatabaseSetup(databaseSetup);
            return databaseParameters;
        }
    }
}
