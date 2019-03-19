package com.sequenceiq.it.cloudbreak.newway.assertion.database;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestTestDto;

public class DatabaseTestTestAssertion {

    private DatabaseTestTestAssertion() {

    }

    public static AssertionV2<DatabaseTestTestDto> validConnectionTest() {
        return (testContext, entity, cloudbreakClient) -> {
            if (!entity.getResponse().getResult().contains("access is denied")) {
                throw new IllegalArgumentException("Database test connection result is not as expected.");
            }
            return entity;
        };
    }
}
