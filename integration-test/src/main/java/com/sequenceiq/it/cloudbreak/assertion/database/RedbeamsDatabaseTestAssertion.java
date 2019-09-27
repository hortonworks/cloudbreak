package com.sequenceiq.it.cloudbreak.assertion.database;

import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;

public class RedbeamsDatabaseTestAssertion {

    private RedbeamsDatabaseTestAssertion() {
    }

    public static Assertion<RedbeamsDatabaseTestDto, RedbeamsClient> containsDatabaseName(String databaseName, Integer expectedCount) {
        return (testContext, entity, redbeamsClient) -> {
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
