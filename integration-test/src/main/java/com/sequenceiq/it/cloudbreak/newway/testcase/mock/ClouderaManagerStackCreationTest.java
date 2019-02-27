package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class ClouderaManagerStackCreationTest extends AbstractClouderaManagerTest {

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
        given = "there is a running cloudbreak",
        when = "a cluster with Cloudera Manager is created",
        then = "the cluster should be available")
    public void createRegularClouderaManagerClusterThenWaitForAvailableThenNoExceptionOccurs(TestContext testContext) {
        String name = testContext.get(ClusterDefinitionTestDto.class).getRequest().getName();
        String cluster = getNameGenerator().getRandomNameForResource();
        String cm = getNameGenerator().getRandomNameForResource();
        String stack = getNameGenerator().getRandomNameForResource();

        testContext
                .given(cm, AmbariEntity.class)
                .withClusterDefinitionName(name)
                .withValidateClusterDefinition(Boolean.FALSE)
                .given(cluster, ClusterEntity.class)
                .withAmbari(cm)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .when(Stack.postV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .validate();
    }
}
