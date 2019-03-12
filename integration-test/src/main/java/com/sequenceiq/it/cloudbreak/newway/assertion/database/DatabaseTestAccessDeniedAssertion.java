package com.sequenceiq.it.cloudbreak.newway.assertion.database;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseTestEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class DatabaseTestAccessDeniedAssertion implements AssertionV2<DatabaseTestEntity> {

    public static DatabaseTestAccessDeniedAssertion getAssertion() {
        return new DatabaseTestAccessDeniedAssertion();
    }

    @Override
    public DatabaseTestEntity doAssertion(TestContext testContext, DatabaseTestEntity entity, CloudbreakClient cloudbreakClient) {
        if (!entity.getResponse().getResult().contains("access is denied")) {
            throw new IllegalArgumentException("Database test connection result is not as expected.");
        }
        return entity;
    }
}
