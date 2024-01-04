package com.sequenceiq.it.cloudbreak.testcase.mock;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import jakarta.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class StackMatrixTest extends AbstractMockTest {

    private static final String CENTOS_7 = "centos7";

    private static final String REDHAT_7 = "redhat7";

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack matrix",
            when = "list all supported stack combinations",
            then = "validate response exists")
    public void testGetStackMatrix(MockedTestContext testContext) {
        testContext
                .given(StackMatrixTestDto.class)
                .when(utilTestClient.stackMatrixV4())
                .then(this::responseExists)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack matrix",
            when = "list all supported stack combinations for a given OS",
            then = "validate response contains only entries with given OS")
    public void testGetStackMatrixWithOs(MockedTestContext testContext) {
        testContext
                .given(StackMatrixTestDto.class)
                    .withOs(CENTOS_7)
                .when(utilTestClient.stackMatrixV4())
                .then(this::responseExists)
                .then(this::responseOnlyContainsSingleOsType)
                .validate();
    }

    private StackMatrixTestDto responseExists(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity);
        assertNotNull(entity.getResponse());
        return entity;
    }

    private StackMatrixTestDto responseOnlyContainsSingleOsType(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        StackMatrixV4Response response = entity.getResponse();
        boolean allSameOsType = response.getCdh().values().stream()
                .allMatch(cmStack -> cmStack.getRepository().getStack().containsKey(REDHAT_7));
        assertTrue(allSameOsType);
        return entity;
    }

}
