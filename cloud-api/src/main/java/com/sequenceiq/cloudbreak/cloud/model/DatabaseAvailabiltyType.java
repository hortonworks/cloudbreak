package com.sequenceiq.cloudbreak.cloud.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;

public class DatabaseAvailabiltyType extends StringType {

    @JsonCreator
    private DatabaseAvailabiltyType(String platform) {
        super(platform);
    }

    public static DatabaseAvailabiltyType databaseAvailabiltyType(String platform) {
        return new DatabaseAvailabiltyType(platform);
    }
}
