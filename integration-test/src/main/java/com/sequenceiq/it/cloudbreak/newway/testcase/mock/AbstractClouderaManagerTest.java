package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.io.IOException;

import com.sequenceiq.it.cloudbreak.newway.client.ClusterDefinitionTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.ResourceUtil;

public abstract class AbstractClouderaManagerTest extends AbstractIntegrationTest {

    @Override
    protected void setupTest(TestContext testContext)  {
        try {
            super.setupTest(testContext);
            testContext.given(ClusterDefinitionTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withClusterDefinition(ResourceUtil.readResourceAsString(applicationContext, "classpath:/clusterdefinition/clouderamanager.bp"))
                    .when(clusterDefinitionTestClient().createV4());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract ClusterDefinitionTestClient clusterDefinitionTestClient();
}
