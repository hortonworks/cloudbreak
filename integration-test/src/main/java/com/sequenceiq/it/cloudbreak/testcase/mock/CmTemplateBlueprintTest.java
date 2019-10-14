package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

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

    private BlueprintTestDto checkBlueprintNameMatches(BlueprintTestDto entity, String initialBlueprintName) {
        CBAssertion.assertEquals(initialBlueprintName, entity.getName());
        return entity;
    }

}
