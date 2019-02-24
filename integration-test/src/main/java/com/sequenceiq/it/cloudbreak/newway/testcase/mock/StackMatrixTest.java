package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.stackmatrix.StackMatrixTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stackmatrix.StackMatrixTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class StackMatrixTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetStackMatrix(MockedTestContext testContext) {
        testContext
                .given(StackMatrixTestDto.class)
                .when(StackMatrixTestAction::getStackMatrix)
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
