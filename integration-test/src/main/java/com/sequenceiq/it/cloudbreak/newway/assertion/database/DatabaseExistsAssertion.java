package com.sequenceiq.it.cloudbreak.newway.assertion.database;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class DatabaseExistsAssertion implements AssertionV2<DatabaseEntity> {

    private String databaseName;

    private Integer expectedCount;

    private DatabaseExistsAssertion(String databaseName, Integer expectedCount) {
        this.databaseName = databaseName;
        this.expectedCount = expectedCount;
    }

    public static DatabaseExistsAssertion getAssertion(String databaseName, Integer expectedCount) {
        return new DatabaseExistsAssertion(databaseName, expectedCount);
    }

    @Override
    public DatabaseEntity doAssertion(TestContext testContext, DatabaseEntity entity, CloudbreakClient cloudbreakClient) {
        boolean countCorrect = entity.getResponses()
                .stream()
                .filter(databaseV4Response -> databaseV4Response.getName().contentEquals(databaseName))
                .count() == expectedCount;
        if (!countCorrect) {
            throw new IllegalArgumentException("Database count for " + databaseName + " is not as expected!");
        }
        return entity;
    }
}
