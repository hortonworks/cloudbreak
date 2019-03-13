package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class ClouderaManagerStackCreationTest extends AbstractClouderaManagerTest {

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster with Cloudera Manager is created",
            then = "the cluster should be available")
    public void testCreateNewRegularCluster(TestContext testContext) {
        String name = testContext.get(ClusterDefinitionTestDto.class).getRequest().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        testContext
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterEntity.class)
                .withClusterDefinitionName(name).withValidateClusterDefinition(Boolean.FALSE).withClouderaManager(clouderaManager)
                .given(StackTestDto.class).withCluster(cluster)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .validate();
    }
}
