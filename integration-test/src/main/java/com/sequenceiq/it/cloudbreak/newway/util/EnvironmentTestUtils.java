package com.sequenceiq.it.cloudbreak.newway.util;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;

public class EnvironmentTestUtils {

    private EnvironmentTestUtils() {
    }

    public static EnvironmentEntity checkRdsAttachedToEnv(TestContext testContext, EnvironmentEntity environment, CloudbreakClient cloudbreakClient) {
        Set<String> rdsConfigs = new HashSet<>();
        Set<DatabaseV4Response> rdsConfigResponseSet = environment.getResponse().getDatabases();
        for (DatabaseV4Response rdsConfigResponse : rdsConfigResponseSet) {
            rdsConfigs.add(rdsConfigResponse.getName());
        }
        if (!rdsConfigs.contains(testContext.get(DatabaseEntity.class).getName())) {
            throw new TestFailException("Rds is not attached to environment");
        }
        return environment;
    }
}
