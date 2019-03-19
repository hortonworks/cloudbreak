package com.sequenceiq.it.cloudbreak.newway.assertion.database;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestTestDto;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class DatabaseTestAccessDeniedAssertion implements AssertionV2<DatabaseTestTestDto> {

    public static DatabaseTestAccessDeniedAssertion getAssertion() {
        return new DatabaseTestAccessDeniedAssertion();
    }

    @Override
    public DatabaseTestTestDto doAssertion(TestContext testContext, DatabaseTestTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        if (!testDto.getResponse().getResult().contains("access is denied")) {
            throw new IllegalArgumentException("Database test connection result is not as expected.");
        }
        return testDto;
    }
}
