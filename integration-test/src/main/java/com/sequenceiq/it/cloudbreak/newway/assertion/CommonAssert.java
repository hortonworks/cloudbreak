package com.sequenceiq.it.cloudbreak.newway.assertion;

import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

public class CommonAssert {

    private CommonAssert() {
    }

    public static <T extends AbstractCloudbreakTestDto> T responseExists(TestContext tc, T entity, CloudbreakClient cc) {
        assertNotNull("Given entity is null!", entity);
        assertNotNull("Response object for " + entity.getClass().getName() + " is null!", entity.getResponse());
        return entity;
    }

    public static <T extends AbstractCloudbreakTestDto> T responsesExists(TestContext tc, T entity, CloudbreakClient cc) {
        assertNotNull("Given entity is null!", entity);
        assertNotNull("Response object for " + entity.getClass().getName() + " is null!", entity.getResponses());
        return entity;
    }

}
