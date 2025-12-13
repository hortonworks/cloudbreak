package com.sequenceiq.it.cloudbreak.assertion;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CommonAssert {

    private CommonAssert() {
    }

    public static <T extends AbstractCloudbreakTestDto> T responseExists(TestContext tc, T entity, CloudbreakClient cc) {
        assertNotNull(entity, "Given entity is null!");
        assertNotNull(entity.getResponse(), "Response object for " + entity.getClass().getName() + " is null!");
        return entity;
    }

    public static <T extends AbstractCloudbreakTestDto> T responsesExists(TestContext tc, T entity, CloudbreakClient cc) {
        assertNotNull(entity, "Given entity is null!");
        assertNotNull(entity.getResponses(), "Response object for " + entity.getClass().getName() + " is null!");
        return entity;
    }

}
