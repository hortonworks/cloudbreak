package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.util.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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
                .then(this::hdpExists)
                .then(this::hdfExists)
                .validate();
    }

    private StackMatrixTestDto responseExists(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity);
        assertNotNull(entity.getResponse());
        return entity;
    }

    private StackMatrixTestDto hdpExists(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity);
        assertNotNull(entity.getResponse());
        assertNotNull(entity.getResponse().getHdp());
        assertFalse(entity.getResponse().getHdp().isEmpty());
        return entity;
    }

    private StackMatrixTestDto hdfExists(TestContext tc, StackMatrixTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity);
        assertNotNull(entity.getResponse());
        assertNotNull(entity.getResponse().getHdf());
        assertFalse(entity.getResponse().getHdf().isEmpty());
        return entity;
    }

}
