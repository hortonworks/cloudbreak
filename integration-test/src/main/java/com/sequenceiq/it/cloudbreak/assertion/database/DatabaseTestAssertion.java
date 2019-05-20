package com.sequenceiq.it.cloudbreak.assertion.database;

import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;

public class DatabaseTestAssertion {

    private DatabaseTestAssertion() {
    }

    public static Assertion<DatabaseTestDto> containsDatabaseName(String databaseName, Integer expectedCount) {
        return (testContext, entity, cloudbreakClient) -> {
            boolean countCorrect = entity.getResponses()
                    .stream()
                    .filter(databaseV4Response -> databaseV4Response.getName().contentEquals(databaseName))
                    .count() == expectedCount;
            if (!countCorrect) {
                throw new IllegalArgumentException("Database count for " + databaseName + " is not as expected!");
            }
            return entity;
        };
    }
}
