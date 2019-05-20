package com.sequenceiq.it.cloudbreak.assertion.database;

import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;

public class DatabaseTestTestAssertion {

    private DatabaseTestTestAssertion() {

    }

    public static Assertion<DatabaseTestTestDto> validConnectionTest() {
        return (testContext, entity, cloudbreakClient) -> {
            if (!entity.getResponse().getResult().contains("access is denied")) {
                throw new IllegalArgumentException("Database test connection result is not as expected.");
            }
            return entity;
        };
    }
}
