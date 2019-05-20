package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.TagSpecificationsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class TagSpecificationsTest extends AbstractIntegrationTest {

    @Inject
    private UtilTestClient utilTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a tag specification",
            when = "list all tag specifications",
            then = "retrieve successfully")
    public void testGetTagSpecifications(MockedTestContext testContext) {
        testContext
                .given(TagSpecificationsTestDto.class)
                .when(utilTestClient.tagSpecificationsV4())
                .validate();
    }

}
