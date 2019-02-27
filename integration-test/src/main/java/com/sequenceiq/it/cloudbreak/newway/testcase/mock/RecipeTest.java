package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.recipe.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class RecipeTest extends AbstractIntegrationTest {

    @Inject
    private LongStringGeneratorUtil longStringGenerator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateValidRecipe(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .when(RecipeTestClient::postV4)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateDeleteValidCreateAgain(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .when(RecipeTestClient::postV4)
                .when(RecipeTestClient::deleteV4)
                .when(RecipeTestClient::postV4)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateSpecialNameRecipe(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .withName(getNameGenerator().getInvalidRandomNameForResource())
                .when(RecipeTestClient::postV4, RunningParameter.key("special-name"))
                .expect(BadRequestException.class, RunningParameter.key("special-name")
                        .withExpectedMessage("The recipe's name can only contain lowercase alphanumeric characters and hyphens and has start "
                                + "with an alphanumeric character"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateAgainRecipe(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .when(RecipeTestClient::postV4)
                .when(RecipeTestClient::postV4, RunningParameter.key("again-name"))
                .expect(BadRequestException.class, RunningParameter.key("again-name").withExpectedMessage("recipe already exists with name '"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateInvalidRecipeShortName(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .withName(longStringGenerator.stringGenerator(3))
                .when(RecipeTestClient::postV4, RunningParameter.key("short-name"))
                .expect(BadRequestException.class, RunningParameter.key("short-name")
                        .withExpectedMessage("The length of the recipe's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateInvalidRecipeLongName(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .withName(longStringGenerator.stringGenerator(101))
                .when(RecipeTestClient::postV4, RunningParameter.key("long-name"))
                .expect(BadRequestException.class, RunningParameter.key("long-name")
                        .withExpectedMessage("The length of the recipe's name has to be in range of 5 to 100"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateInvalidRecipeLongDescr(TestContext testContext) {
        testContext.given(RecipeTestDto.class)
                .withDescription(longStringGenerator.stringGenerator(1001))
                .when(RecipeTestClient::postV4, RunningParameter.key("long-desc"))
                .expect(BadRequestException.class, RunningParameter.key("long-desc").withExpectedMessage("size must be between 0 and 1000"))
                .validate();
    }
}
