package com.sequenceiq.environment.parameter.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonDeserialize(builder = AzureDatabaseParametersDto.Builder.class)
public class AzureDatabaseParametersDto {

    private final DatabaseSetup databaseSetup;

    public AzureDatabaseParametersDto(Builder builder) {
        databaseSetup = builder.databaseSetup;
    }

    public DatabaseSetup getDatabaseSetup() {
        return databaseSetup;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AzureDatabaseParametersDto{" +
                "databaseSetup=" + databaseSetup +
                '}';
    }

    @JsonPOJOBuilder
    public static final class Builder {

        private DatabaseSetup databaseSetup;

        public Builder withDatabaseSetup(DatabaseSetup databaseSetup) {
            this.databaseSetup = databaseSetup;
            return this;
        }

        public AzureDatabaseParametersDto build() {
            return new AzureDatabaseParametersDto(this);
        }
    }
}