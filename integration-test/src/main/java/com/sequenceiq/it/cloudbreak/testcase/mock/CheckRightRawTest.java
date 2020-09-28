package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.RawCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CheckRightRawTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the util API",
            when = "checkRight method is called with nonexisting enum value",
            then = "API should check for default value")
    public void checkDefaultEnumHandling(MockedTestContext testContext) {
        testContext
                .given(RawCloudbreakTestDto.class)
                .withRequest("{\"rights\":[\"ENV_CREATE\",\"DISTROX_READ\"]}")
                .when(utilTestClient.checkRightRaw())
                .then((context, dto, client) -> {
                    Assertions.assertThat(dto.getResponse()).contains("ENVIRONMENT_READ");
                    return dto;
                })
                .validate();
    }

}
