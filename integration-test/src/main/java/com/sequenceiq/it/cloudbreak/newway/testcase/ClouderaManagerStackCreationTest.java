package com.sequenceiq.it.cloudbreak.newway.testcase;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.util.ResourceUtil;

public class ClouderaManagerStackCreationTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) throws IOException {
        TestContext testContext = (TestContext) data[0];

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        testContext.given(BlueprintEntity.class)
                .withName(getNameGenerator().getRandomNameForMock())
                .withAmbariBlueprint(ResourceUtil.readResourceAsString(applicationContext, "classpath:/blueprint/clouderamanager.bp"))
                .when(Blueprint.postV2());
    }

    @Test(dataProvider = "testContext")
    public void testCreateNewRegularCluster(TestContext testContext) {
        String name = testContext.get(BlueprintEntity.class).getRequest().getName();
        testContext
                .given("cm", AmbariEntity.class).withBlueprintName(name).withValidateBlueprint(Boolean.FALSE)
                .given("cmcluster", ClusterEntity.class).withAmbari("cm")
                .given(StackEntity.class).withCluster("cmcluster")
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
