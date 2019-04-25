package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.io.IOException;

import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.ResourceUtil;

public abstract class AbstractClouderaManagerTest extends AbstractIntegrationTest {

    @Override
    protected void setupTest(TestContext testContext)  {
        try {
            super.setupTest(testContext);
            testContext.given(BlueprintTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withBlueprint(ResourceUtil.readResourceAsString(applicationContext, "classpath:/blueprint/clouderamanager.bp"))
                    .when(blueprintTestClient().createV4());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract BlueprintTestClient blueprintTestClient();
}
