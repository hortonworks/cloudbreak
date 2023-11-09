package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import java.io.IOException;

import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;
import com.sequenceiq.it.util.ResourceUtil;

public abstract class AbstractClouderaManagerTest extends AbstractMockTest {

    public static final String PROFILE_RETURN_HTTP_500 = "HTTP_500";

    @Override
    protected void setupTest(TestContext testContext) {
        super.setupTest(testContext);
        createCmBlueprint(testContext);
    }

    protected void createCmBlueprint(TestContext testContext) {
        try {
            String bp = ResourceUtil
                    .readResourceAsString(applicationContext, "classpath:/blueprint/clouderamanager.bp")
                    .replaceAll("CDH_RUNTIME", commonClusterManagerProperties().getRuntimeVersion());

            testContext.given(BlueprintTestDto.class)
                    .withName(resourcePropertyProvider().getName())
                    .withBlueprint(bp)
                    .when(blueprintTestClient().createV4());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected abstract BlueprintTestClient blueprintTestClient();
}
