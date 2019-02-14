package com.sequenceiq.it.cloudbreak.newway.testcase;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.it.cloudbreak.newway.ClusterDefinition;
import com.sequenceiq.it.cloudbreak.newway.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.util.ResourceUtil;

public class AbstractClouderaManagerTest extends AbstractIntegrationTest {
    @BeforeMethod
    public void beforeMethod(Object[] data) throws IOException {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        testContext.given(ClusterDefinitionEntity.class)
                .withName(getNameGenerator().getRandomNameForMock())
                .withAmbariBlueprint(ResourceUtil.readResourceAsString(applicationContext, "classpath:/blueprint/clouderamanager.bp"))
                .when(ClusterDefinition.postV2());
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
