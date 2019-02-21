package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.blueprint.BlueprintTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintTestDto;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class BlueprintTest extends AbstractIntegrationTest {

    private static final String INVALID_SHORT_BP_NAME = "";

    private static final String VALID_BP = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"}],\"cardinality\":\"1"
            + "\"}]}";

    private static final String ILLEGAL_BP_NAME = "Illegal blueprint name %s";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateBlueprint(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForMock();
        List<String> keys = Arrays.asList("key_1", "key_2", "key_3");
        List<Object> values = Arrays.asList("value_1", "value_2", "value_3");
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withDescription(blueprintName)
                .withTag(keys, values)
                .withAmbariBlueprint(VALID_BP)
                .when(BlueprintTestAction::postV4, key(blueprintName))
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
    public void testCreateAgainBlueprint(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint(VALID_BP)
                .when(BlueprintTestAction::postV4)
                .when(BlueprintTestAction::postV4, key("duplicate-error"))
                .expect(BadRequestException.class, expectedMessage("blueprint already exists with name ").withKey("duplicate-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateInvalidShortBlueprint(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint(VALID_BP).withName(INVALID_SHORT_BP_NAME)
                .when(BlueprintTestAction::postV4, key("shortname-error"))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the blueprint's name has to be in range of 1 to 100 and should not contain semicolon and "
                                + "percentage character.")
                                .withKey("shortname-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateInvalidLongBlueprint(TestContext testContext) {
        String invalidLongName = longStringGeneratorUtil.stringGenerator(101);
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint(VALID_BP).withName(invalidLongName)
                .when(BlueprintTestAction::postV4, key("longname-error"))
                .expect(BadRequestException.class,
                        expectedMessage("The length of the blueprint's name has to be in range of 1 to 100 and should not contain semicolon and "
                                + "percentage character.")
                                .withKey("longname-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateLongDescription(TestContext testContext) {
        String invalidLongDescripton = longStringGeneratorUtil.stringGenerator(1001);
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint(VALID_BP).withDescription(invalidLongDescripton)
                .when(BlueprintTestAction::postV4, key("longdesc-error"))
                .expect(BadRequestException.class,
                        expectedMessage("size must be between 0 and 1000")
                                .withKey("longdesc-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateEmptyJSONBlueprint(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint("{}")
                .when(BlueprintTestAction::postV4, key("emptybp-error"))
                .expect(BadRequestException.class,
                        expectedMessage("'Blueprints' node is missing from JSON.")
                                .withKey("emptybp-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateEmptyFileBlueprint(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint(getBlueprintUrl())
                .when(BlueprintTestAction::postV4, key("emptyfilebp-error"))
                .expect(BadRequestException.class,
                        expectedMessage("Failed to parse JSON.")
                                .withKey("emptyfilebp-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateInvalidURLBlueprintException(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .withAmbariBlueprint(getBlueprintInvalidUrl())
                .when(BlueprintTestAction::postV4, key("invalidurlbp-error"))
                .expect(BadRequestException.class,
                        expectedMessage("Failed to parse JSON.")
                                .withKey("invalidurlbp-error"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateBlueprintWithInvalidCharacterName(TestContext testContext) {
        String blueprintName = getNameGenerator().getInvalidRandomNameForMock();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withAmbariBlueprint(VALID_BP)
                .when(BlueprintTestAction::postV4, key(blueprintName))
                .expect(BadRequestException.class, expectedMessage("must match ").withKey(blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateBlueprintWithInvalidJson(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForMock();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withAmbariBlueprint("apple-tree")
                .when(BlueprintTestAction::postV4, key(blueprintName))
                .expect(BadRequestException.class, expectedMessage("Failed to parse JSON").withKey(blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testListBlueprint(TestContext testContext) {
        testContext.given(BlueprintTestDto.class)
                .when(BlueprintTestAction::listV4)
                .then(BlueprintTest::checkDefaultBlueprintsIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetSpecificBlueprint(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForMock();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withAmbariBlueprint(VALID_BP)
                .when(BlueprintTestAction::postV4, key(blueprintName))
                .when(BlueprintTestAction::getV4, key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(blueprintName, entity.getName());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testDeleteSpecificBlueprint(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForMock();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withAmbariBlueprint(VALID_BP)
                .when(BlueprintTestAction::postV4, key(blueprintName))
                .when(BlueprintTestAction::deleteV4, key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(blueprintName, entity.getName());
                    return entity;
                })
                .when(BlueprintTestAction::listV4)
                .then(BlueprintTest::checkBlueprintDoesNotExistInTheList)
                .validate();
    }

    private static BlueprintTestDto checkBlueprintDoesNotExistInTheList(TestContext testContext, BlueprintTestDto entity, CloudbreakClient cloudbreakClient) {
        if (entity.getViewResponses().stream().anyMatch(bp -> bp.getName().equals(entity.getName()))) {
            throw new TestFailException(
                    String.format("Blueprint is still exist in the db %s", entity.getName()));
        }
        return entity;
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testRequestSpecificBlueprintRequest(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForMock();
        testContext.given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withAmbariBlueprint(VALID_BP)
                .when(BlueprintTestAction::postV4, key(blueprintName))
                .when(BlueprintTestAction::requestV4, key(blueprintName))
                .then((tc, entity, cc) -> {
                    assertEquals(entity.getRequest().getAmbariBlueprint(), VALID_BP);
                    assertEquals(blueprintName, entity.getName());
                    return entity;
                })
                .validate();
    }

    private static BlueprintTestDto checkDefaultBlueprintsIsListed(TestContext testContext, BlueprintTestDto blueprint, CloudbreakClient cloudbreakClient) {
        List<BlueprintV4ViewResponse> result = blueprint.getViewResponses().stream()
                .filter(bp -> bp.getStatus().equals(ResourceStatus.DEFAULT))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Blueprint is not listed");
        }
        return blueprint;
    }

    private <O extends Object> boolean assertList(Collection<O> result, Collection<O> expected) {
        return result.containsAll(expected) && result.size() == expected.size();
    }

    private String getBlueprintUrl() {
        return "https://rawgit.com/hortonworks/cloudbreak/master/integration-test/src/main/resources/"
                + "blueprint/multi-node-hdfs-yarn.bp";
    }

    private String getBlueprintInvalidUrl() {
        return "https://github.com";
    }
}
