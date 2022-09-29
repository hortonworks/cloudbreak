package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Optional;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.it.cloudbreak.assertion.CBAssertion;
import com.sequenceiq.it.cloudbreak.assertion.audit.BlueprintAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;

public class CmTemplateBlueprintTest extends BlueprintTestBase {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private BlueprintAuditGrpcServiceAssertion blueprintAuditGrpcServiceAssertion;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid CM Template based blueprint create request is sent",
            then = "the blueprint should be in the response")
    public void testCreateCMBlueprint(MockedTestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withDescription(blueprintName)
                .withBlueprint(super.getValidCMTemplateText())
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .then((tc, entity, cc) -> checkBlueprintNameMatches(entity, blueprintName))
                .when(blueprintTestClient.listV4())
                .then((tc, entity, cc) -> checkBlueprintLastUpdatedNotNull(entity))
                .when(blueprintTestClient.deleteV4())
                .then(blueprintAuditGrpcServiceAssertion::create)
                .then(blueprintAuditGrpcServiceAssertion::delete)
                .validate();
    }

    private BlueprintTestDto checkBlueprintNameMatches(BlueprintTestDto entity, String initialBlueprintName) {
        CBAssertion.assertEquals(initialBlueprintName, entity.getName());
        return entity;
    }

    private BlueprintTestDto checkBlueprintLastUpdatedNotNull(BlueprintTestDto entity) {
        Optional<BlueprintV4ViewResponse> bpResponse =  entity.getViewResponses().stream().findFirst();
        CBAssertion.assertTrue(bpResponse.isPresent() && bpResponse.get().getLastUpdated() > 0);
        return entity;
    }
}
