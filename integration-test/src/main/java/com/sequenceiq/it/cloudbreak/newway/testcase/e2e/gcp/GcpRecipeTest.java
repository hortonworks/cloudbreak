package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.gcp;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;
import static org.junit.Assert.assertEquals;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.SshService;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;

public class GcpRecipeTest extends AbstractE2ETest {

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private SshService sshService;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    private String recipeName;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a stack is created with a python recipe",
            then = "the stack should be available AND the python recipe must have executed properly")
    public void testClusterWithPythonRecipe(TestContext testContext) {
        recipeName = resourcePropertyProvider.getName();
        testContext.given(RecipeTestDto.class)
                .withName(recipeName)
                .withContent(Base64.encodeBase64String(commonCloudProperties.getRecipe().getContent().getBytes()))
                .withRecipeType(RecipeV4Type.PRE_AMBARI_START)
                .when(recipeTestClient.createV4())

                .given(MASTER.name(), InstanceGroupTestDto.class)
                .withHostGroup(MASTER)
                .withNodeCount(1)
                .withRecipes(recipeName)

                .given(WORKER.name(), InstanceGroupTestDto.class)
                .withHostGroup(WORKER)
                .withNodeCount(1)
                .withRecipes(recipeName)

                .given(StackTestDto.class)
                .replaceInstanceGroups(WORKER.name(), MASTER.name())
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(this::assertRecipes)
                .validate();
    }

    private StackTestDto assertRecipes(TestContext testContext, StackTestDto stack, CloudbreakClient cloudbreakClient) {
        StackV4Response response = stack.getResponse();
        assertHostgroup(response, MASTER);
        assertHostgroup(response, WORKER);
        return stack;
    }

    private void assertHostgroup(StackV4Response response, HostGroupType hostgroup) {
        Optional<InstanceGroupV4Response> masterHostgroup = response.getInstanceGroups().stream()
                .filter(ig -> hostgroup.name().equalsIgnoreCase(ig.getName()))
                .findFirst();
        masterHostgroup.ifPresentOrElse(this::assertRecipeOnHostgroup, () -> Assert.fail(hostgroup + " hostgroup is missing!"));
    }

    private void assertRecipeOnHostgroup(InstanceGroupV4Response hostgroup) {
        assertEquals(1, hostgroup.getRecipes().size());
        assertEquals(recipeName, hostgroup.getRecipes().get(0).getName());
        String recipeContent = new String(Base64.decodeBase64(hostgroup.getRecipes().get(0).getContent()));
        assertEquals(commonCloudProperties.getRecipe().getContent(), recipeContent);
        InstanceMetaDataV4Response metadata = hostgroup.getMetadata().iterator().next();
        String recipeOutput = sshService.getRecipeExecutionOutput(metadata.getPublicIp(), "cloudbreak",
                commonCloudProperties.getRecipe().getOutputFilePath());
        assertEquals(commonCloudProperties.getRecipe().getOutput().trim(), recipeOutput.trim());
    }
}
