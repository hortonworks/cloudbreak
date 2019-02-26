package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.newway.assertion.CBAssertion.assertTrue;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ClusterDefinitionTest extends AbstractIntegrationTest {

    private static final String VALID_CD = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"}],\"cardinality\":\"1"
            + "\"}]}";

    @Override
    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
    }

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
    public void testCreateClusterDefinition(TestContext testContext) {
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        List<String> keys = Arrays.asList("key_1", "key_2", "key_3");
        List<Object> values = Arrays.asList("value_1", "value_2", "value_3");
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withDescription(clusterDefinitionName)
                .withTag(keys, values)
                .withClusterDefinition(VALID_CD)
                .when(ClusterDefinitionTestAction.postV4(), key(clusterDefinitionName))
                .then((tc, entity, cc) -> {
                    assertEquals(clusterDefinitionName, entity.getName());
                    assertEquals(clusterDefinitionName, entity.getDescription());
                    assertTrue(assertList(entity.getTag().keySet(), keys));
                    assertTrue(assertList(entity.getTag().values(), values));
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateClusterDefinitionWithInvalidCharacterName(TestContext testContext) {
        String clusterDefinitionName = getNameGenerator().getInvalidRandomNameForResource();
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withClusterDefinition(VALID_CD)
                .when(ClusterDefinitionTestAction.postV4(), key(clusterDefinitionName))
                .expect(BadRequestException.class, expectedMessage("must match ").withKey(clusterDefinitionName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testCreateClusterDefinitionWithInvalidJson(TestContext testContext) {
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withClusterDefinition("apple-tree")
                .when(ClusterDefinitionTestAction.postV4(), key(clusterDefinitionName))
                .expect(BadRequestException.class, expectedMessage("Failed to parse JSON").withKey(clusterDefinitionName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testListClusterDefinition(TestContext testContext) {
        testContext.given(ClusterDefinitionTestDto.class)
                .when(ClusterDefinitionTestAction.listV4())
                .then(ClusterDefinitionTest::checkDefaultClusterDefinitionsIsListed)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testGetSpecificClusterDefinition(TestContext testContext) {
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withClusterDefinition(VALID_CD)
                .when(ClusterDefinitionTestAction.postV4(), key(clusterDefinitionName))
                .when(ClusterDefinitionTestAction.getV4(), key(clusterDefinitionName))
                .then((tc, entity, cc) -> {
                    assertEquals(clusterDefinitionName, entity.getName());
                    return entity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testDeleteSpecificClusterDefinition(TestContext testContext) {
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withClusterDefinition(VALID_CD)
                .when(ClusterDefinitionTestAction.postV4(), key(clusterDefinitionName))
                .when(ClusterDefinitionTestAction.deleteV4(), key(clusterDefinitionName))
                .then((tc, entity, cc) -> {
                    assertEquals(clusterDefinitionName, entity.getName());
                    return entity;
                })
                .when(ClusterDefinitionTestAction.listV4())
                .then(ClusterDefinitionTest::checkClusterDefinitionDoesNotExistInTheList)
                .validate();
    }

    private static ClusterDefinitionTestDto checkClusterDefinitionDoesNotExistInTheList(TestContext testContext, ClusterDefinitionTestDto entity,
            CloudbreakClient cloudbreakClient) {
        if (entity.getViewResponses().stream().anyMatch(bp -> bp.getName().equals(entity.getName()))) {
            throw new TestFailException(
                    String.format("Cluster definition is still exist in the db %s", entity.getName()));
        }
        return entity;
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testRequestSpecificClusterDefinitionRequest(TestContext testContext) {
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        testContext.given(ClusterDefinitionTestDto.class)
                .withName(clusterDefinitionName)
                .withClusterDefinition(VALID_CD)
                .when(ClusterDefinitionTestAction.postV4(), key(clusterDefinitionName))
                .when(ClusterDefinitionTestAction.requestV4(), key(clusterDefinitionName))
                .then((tc, entity, cc) -> {
                    assertEquals(entity.getRequest().getClusterDefinition(), VALID_CD);
                    assertEquals(clusterDefinitionName, entity.getName());
                    return entity;
                })
                .validate();
    }

    private static ClusterDefinitionTestDto checkDefaultClusterDefinitionsIsListed(TestContext testContext, ClusterDefinitionTestDto clusterDefinition,
            CloudbreakClient cloudbreakClient) {
        List<ClusterDefinitionV4ViewResponse> result = clusterDefinition.getViewResponses().stream()
                .filter(bp -> bp.getStatus().equals(ResourceStatus.DEFAULT))
                .collect(Collectors.toList());
        if (result.isEmpty()) {
            throw new TestFailException("Cluster definition is not listed");
        }
        return clusterDefinition;
    }

    private  <O extends Object> boolean assertList(Collection<O> result, Collection<O> expected) {
        return result.containsAll(expected) && result.size() == expected.size();
    }
}
