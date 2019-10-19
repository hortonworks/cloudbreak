package com.sequenceiq.it.cloudbreak.testcase.mock;

import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class StackMatrixTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "stack matrix",
            when = "list all supported stack combinations",
            then = "validate hdp and hdf ambari descriptor")
    public void testGetStackMatrix(MockedTestContext testContext) {
        testContext
                .given(StackMatrixTestDto.class)
                .when(utilTestClient.stackMatrixV4())
                .then(this::responseExists)
                .validate();
    }

    private StackMatrixTestDto responseExists(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity);
        assertNotNull(entity.getResponse());
        return entity;
    }

}
