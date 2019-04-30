package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.newway.assertion.CBAssertion.assertTrue;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class BlueprintTest extends AbstractIntegrationTest {

    private static final String VALID_CD = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"}],\"cardinality\":\"1"
            + "\"}]}";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid blueprint create request is sent",
            then = "the blueprint should be in the response")
    public void testCreateBlueprint(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        List<String> keys = Arrays.asList("key_1", "key_2", "key_3");
        List<Object> values = Arrays.asList("value_1", "value_2", "value_3");
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withDescription(blueprintName)
                .withTag(keys, values)
                .withBlueprint(VALID_CD)
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(blueprintName, entity.getName());
                    assertEquals(blueprintName, entity.getDescription());
                    assertTrue(assertList(entity.getTag().keySet(), keys));
                    assertTrue(assertList(entity.getTag().values(), values));
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a blueprint create request with invalid name is sent",
            then = "a BadRequestException should be returned")
    public void testCreateBlueprintWithInvalidCharacterName(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getInvalidName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(VALID_CD)
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .expect(BadRequestException.class, expectedMessage("must match ").withKey(blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a blueprint create request is sent with an invalid JSON",
            then = "a BadRequestException should be returned with 'Failed to parse JSON' message")
    public void testCreateBlueprintWithInvalidJson(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint("apple-tree")
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .expect(BadRequestException.class, expectedMessage("Failed to parse JSON").withKey(blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "the list blueprint",
            then = "returns with blueprint list")
    public void testListBlueprint(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4())
                .then(BlueprintTest::checkDefaultBlueprintsIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "the get blueprint endpoint is called",
            then = "the blueprint should be returned")
    public void testGetSpecificBlueprint(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(VALID_CD)
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .when(blueprintTestClient.getV4(), key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(blueprintName, entity.getName());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a specified blueprint",
            when = "delete blueprint is called for the specified blueprint",
            then = "the blueprint list response does not contain it")
    public void testDeleteSpecificBlueprint(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(VALID_CD)
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .when(blueprintTestClient.deleteV4(), key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(blueprintName, entity.getName());
                    return entity;
                })
                .when(blueprintTestClient.listV4())
                .then(BlueprintTest::checkBlueprintDoesNotExistInTheList)
                .validate();
    }

    private static BlueprintTestDto checkBlueprintDoesNotExistInTheList(TestContext testContext, BlueprintTestDto entity,
            CloudbreakClient cloudbreakClient) {
        if (entity.getViewResponses().stream().anyMatch(bp -> bp.getName().equals(entity.getName()))) {
            throw new TestFailException(
                    String.format("Blueprint still exists in the db %s", entity.getName()));
        }
        return entity;
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a blueprint",
            when = "the blueprint request endpoint is called",
            then = "the valid request for thet blueprint is returned")
    public void testRequestSpecificBlueprintRequest(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(VALID_CD)
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .when(blueprintTestClient.requestV4(), key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(entity.getRequest().getBlueprint(), VALID_CD);
                    assertEquals(blueprintName, entity.getName());
                    return entity;
                })
                .validate();
    }

    private static BlueprintTestDto checkDefaultBlueprintsIsListed(TestContext testContext, BlueprintTestDto blueprint,
            CloudbreakClient cloudbreakClient) {
        List<BlueprintV4ViewResponse> result = blueprint.getViewResponses().stream()
                .filter(bp -> bp.getStatus().equals(ResourceStatus.DEFAULT))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Default blueprint is not listed");
        }
        return blueprint;
    }

    private <O extends Object> boolean assertList(Collection<O> result, Collection<O> expected) {
        return result.containsAll(expected) && result.size() == expected.size();
    }
}
