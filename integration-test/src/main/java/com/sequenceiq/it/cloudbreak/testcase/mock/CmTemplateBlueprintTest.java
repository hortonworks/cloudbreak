package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.CBAssertion;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;

public class CmTemplateBlueprintTest extends BlueprintTestBase {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid CM Template based blueprint create request is sent",
            then = "the blueprint should be in the response")
    public void testCreateCMBlueprint(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withDescription(blueprintName)
                .withBlueprint(super.getValidCMTemplateText())
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .then((tc, entity, cc) -> checkBlueprintNameMatches(entity, blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a user managed blueprint is registered",
            when = "listing all blueprints",
            then = "the blueprint should be in the list")
    public void testCreateAndListBlueprint(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(super.getValidAmbariBlueprintText())
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .when(blueprintTestClient.listV4())
                .then((tc, entity, cc) -> checkBlueprintExistsInList(entity, blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a user managed blueprint is registered",
            when = "listing all blueprints",
            then = "the blueprint should be in the list")
    public void testCreateBlueprintWithTags(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        List<String> keys = Arrays.asList("key_1", "key_2", "key_3");
        List<Object> values = Arrays.asList("value_1", "value_2", "value_3");
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withDescription(blueprintName)
                .withTag(keys, values)
                .withBlueprint(super.getValidAmbariBlueprintText())
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .when(blueprintTestClient.listV4())
                .then((tc, entity, cc) -> checkBlueprintTagsAreTheSame(entity, keys, values))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a user managed blueprint is registered",
            when = "listing all blueprints",
            then = "the blueprint should be in the list")
    public void testCreateBlueprintWithDescription(TestContext testContext) {
        String description = "some fancy description";
        testContext.given(BlueprintTestDto.class)
                .withDescription(description)
                .withBlueprint(super.getValidAmbariBlueprintText())
                .when(blueprintTestClient.createV4())
                .when(blueprintTestClient.getV4())
                .then((tc, entity, cc) -> checkBlueprintDescriptionMatches(entity, description))
                .validate();
    }

    private BlueprintTestDto checkBlueprintNameMatches(BlueprintTestDto entity, String initialBlueprintName) {
        CBAssertion.assertEquals(initialBlueprintName, entity.getName());
        return entity;
    }

    private BlueprintTestDto checkBlueprintExistsInList(BlueprintTestDto entity, String blueprintName) {
        CBAssertion.assertTrue(entity.getViewResponses().stream().anyMatch(bp -> blueprintName.equals(bp.getName())));
        return entity;
    }

    private BlueprintTestDto checkBlueprintDescriptionMatches(BlueprintTestDto entity, String expectedDescription) {
        CBAssertion.assertTrue(expectedDescription.equals(entity.getDescription()));
        return entity;
    }

    private BlueprintTestDto checkBlueprintTagsAreTheSame(BlueprintTestDto entity, List<String> keys, List<Object> values) {
        CBAssertion.assertTrue(assertList(entity.getTag().keySet(), keys));
        CBAssertion.assertTrue(assertList(entity.getTag().values(), values));
        return entity;
    }

}
