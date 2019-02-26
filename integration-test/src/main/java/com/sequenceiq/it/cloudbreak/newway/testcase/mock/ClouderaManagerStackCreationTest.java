package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;

public class ClouderaManagerStackCreationTest extends AbstractClouderaManagerTest {
    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateNewRegularCluster(TestContext testContext) {
        String name = testContext.get(ClusterDefinitionTestDto.class).getRequest().getName();
        testContext
                .given("cm", AmbariEntity.class).withClusterDefinitionName(name).withValidateClusterDefinition(Boolean.FALSE)
                .given("cmcluster", ClusterEntity.class).withAmbari("cm")
                .given(StackTestDto.class).withCluster("cmcluster")
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .validate();
    }
}
