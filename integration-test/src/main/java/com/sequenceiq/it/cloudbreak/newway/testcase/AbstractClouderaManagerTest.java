package com.sequenceiq.it.cloudbreak.newway.testcase;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.blueprint.BlueprintTestAction;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintTestDto;
import com.sequenceiq.it.util.ResourceUtil;

public class AbstractClouderaManagerTest extends AbstractIntegrationTest {
    @BeforeMethod
    public void beforeMethod(Object[] data) throws IOException {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
        testContext.given(BlueprintTestDto.class)
                .withName(getNameGenerator().getRandomNameForMock())
                .withAmbariBlueprint(ResourceUtil.readResourceAsString(applicationContext, "classpath:/blueprint/clouderamanager.bp"))
                .when(BlueprintTestAction::postV4);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
