package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.it.cloudbreak.newway.client.ClusterDefinitionTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.ResourceUtil;

public abstract class AbstractClouderaManagerTest extends AbstractIntegrationTest {
    @BeforeMethod
    public void beforeMethod(Object[] data) throws IOException {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(getNameGenerator().getRandomNameForResource())
                .withClusterDefinition(ResourceUtil.readResourceAsString(applicationContext, "classpath:/clusterdefinition/clouderamanager.bp"))
                .when(clusterDefinitionTestClient().createV4());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    protected abstract ClusterDefinitionTestClient clusterDefinitionTestClient();
}
