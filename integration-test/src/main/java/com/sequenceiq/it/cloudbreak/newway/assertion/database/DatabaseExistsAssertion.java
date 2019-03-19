package com.sequenceiq.it.cloudbreak.newway.assertion.database;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class DatabaseExistsAssertion implements AssertionV2<DatabaseTestDto> {

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
    public DatabaseTestDto doAssertion(TestContext testContext, DatabaseTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        boolean countCorrect = testDto.getResponses()
                .stream()
                .filter(databaseV4Response -> databaseV4Response.getName().contentEquals(databaseName))
                .count() == expectedCount;
        if (!countCorrect) {
            throw new IllegalArgumentException("Database count for " + databaseName + " is not as expected!");
        }
        return testDto;
    }
}
