package com.sequenceiq.it.cloudbreak.testcase.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.testng.annotations.Test;
import org.testng.collections.Lists;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.ClusterTemplateTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.mock.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.config.user.TestUserCreator;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class MockGlobalDefaultClusterTemplateTest extends AbstractMockTest {

    private static final String ACCOUNT_TEMPLATE = "template-test-%s";

    private static final String GLOBAL_CLUSTER_DEF_CRN_PREFIX = "crn:cdp:datahub:us-west-1:cloudera_default:clusterdefinition:";

    private static final String ACCOUNT_DEFAULT_CLUSTER_DEF_CRN = "crn:cdp:datahub:us-west-1:%s:clusterdefinition:";

    private static final String GLOBAL_BLUEPRINT_CRN_PREFIX = "crn:cdp:datahub:us-west-1:cloudera_default:clustertemplate:";

    private static final String ACCOUNT_DEFAULT_BLUEPRINT_CRN = "crn:cdp:datahub:us-west-1:%s:clustertemplate:";

    private static final String CLUSTER_TEMPLATE_NAME = "7.3.2 - Data Engineering HA - Spark3 for AWS";

    private static final String LAKE_HOUSE_CLUSTER_TEMPLATE_NAME = "7.3.2 - Lakehouse Optimizer for AWS";

    private static final String BLUEPRINT_NAME = "7.3.2 - Data Engineering: HA: Apache Spark3, Apache Hive, Apache Oozie";

    private static final String LAKE_HOUSE_BLUEPRINT_NAME = "7.3.2 - Lakehouse Optimizer";

    private static final int WAIT_ENTITLEMENT_SECONDS = 5;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private TestUserCreator testUserCreator;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private ClusterTemplateTestClient clusterTemplateTestClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.as(testUserCreator.createAdmin(newAccount(), "admin1"));
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is granted",
            when = "a DistroX with Cloudera Manager is created",
            then = "all blueprints and cluster templates should be global default")
    public void testClusterCreationWhenGlobalTemplateEnabled(MockedTestContext testContext) {
        grantGlobalTemplateEntitlement(testContext);

        createDefaultDatahub(testContext);
        validateAllClusterTemplates(testContext, globalDefaultClusterTemplate());
        validateAllBlueprints(testContext, globalDefaultBlueprint());
        validateUsedBlueprint(testContext, globalDefaultBlueprint());
        repairDataHub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is disabled",
            when = "a DistroX with Cloudera Manager is created",
            and = "the global template entitlement is granted later",
            then = "all blueprints and cluster templates should be global default but the cluster should still us account default blueprint")
    public void testClusterOperationsWhenGlobalTemplateEnabledAfterCreation(MockedTestContext testContext) {
        revokeEntitlement(testContext, Entitlement.CDP_GLOBAL_DEFAULT_TEMPLATE);

        createDefaultDatahub(testContext);
        validateAllClusterTemplates(testContext, accountDefaultClusterTemplate(testContext));
        validateAllBlueprints(testContext, accountDefaultBlueprint(testContext));
        validateUsedBlueprint(testContext, accountDefaultBlueprint(testContext));

        grantGlobalTemplateEntitlement(testContext);

        validateAllClusterTemplates(testContext, globalDefaultClusterTemplate());
        validateAllBlueprints(testContext, globalDefaultBlueprint());
        validateUsedBlueprint(testContext, accountDefaultBlueprint(testContext));
        repairDataHub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is revoked",
            when = "new cluster template is created",
            then = "all cluster templates are account default")
    public void testListClusterTeamplatesWithAccountDefaults(MockedTestContext testContext) {
        revokeEntitlement(testContext, Entitlement.CDP_GLOBAL_DEFAULT_TEMPLATE);

        createDefaultEnvironment(testContext);
        createDefaultClusterTemplate(testContext);

        validateAllClusterTemplates(testContext, accountDefaultClusterTemplate(testContext)
                .or(template -> template.getCrn().equals(testContext.given(ClusterTemplateTestDto.class).getCrn())));
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is granted",
            when = "new cluster template is created",
            then = "all default cluster templates are global default")
    public void testListClusterTeamplatesWithGlobalDefaults(MockedTestContext testContext) {
        grantGlobalTemplateEntitlement(testContext);

        createDefaultEnvironment(testContext);
        createDefaultClusterTemplate(testContext);

        validateAllClusterTemplates(testContext, globalDefaultClusterTemplate()
                .or(template -> template.getCrn().equals(testContext.given(ClusterTemplateTestDto.class).getCrn())));
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is revoked and later granted",
            when = "the cluster templates are listed",
            then = "list returns the same number of templates")
    public void testListClusterTemplatesShowsTheSameResults(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        revokeEntitlement(testContext, Entitlement.CDP_GLOBAL_DEFAULT_TEMPLATE);

        validateAllClusterTemplates(testContext, accountDefaultClusterTemplate(testContext));
        int numberOfAccountDefaultClusterTemplates = getNumberOfTemplates(testContext);

        grantGlobalTemplateEntitlement(testContext);

        validateAllClusterTemplates(testContext, globalDefaultClusterTemplate());
        int numberOfGlobalClusterTemplates = getNumberOfTemplates(testContext);

        assertNotEquals(0, numberOfGlobalClusterTemplates);
        assertNotEquals(0, numberOfAccountDefaultClusterTemplates);
        assertEquals(numberOfGlobalClusterTemplates, numberOfAccountDefaultClusterTemplates);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is revoked and later granted and lakehouse and internal tenant is disabled",
            when = "the cluster templates are listed",
            then = "list returns the same number of templates")
    public void testListClusterTemplatesShowsTheSameResultsWhenNoLakeHouseAndNoInternalTenant(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        revokeEntitlement(testContext, Entitlement.CDP_LAKEHOUSE_OPTIMIZER_ENABLED);
        revokeEntitlement(testContext, Entitlement.CLOUDERA_INTERNAL_ACCOUNT);

        validateAllClusterTemplates(testContext, accountDefaultClusterTemplate(testContext));
        int numberOfAccountDefaultClusterTemplates = getNumberOfTemplates(testContext);

        grantGlobalTemplateEntitlement(testContext);

        validateAllClusterTemplates(testContext, globalDefaultClusterTemplate());
        int numberOfGlobalClusterTemplates = getNumberOfTemplates(testContext);

        assertNotEquals(0, numberOfGlobalClusterTemplates);
        assertNotEquals(0, numberOfAccountDefaultClusterTemplates);
        assertEquals(numberOfAccountDefaultClusterTemplates, numberOfGlobalClusterTemplates);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is granted",
            when = "get one cluster template and one blueprint",
            then = "all of them are global default")
    public void testGetClusterTemplateWhenGlobalDefaultEnabled(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        grantGlobalTemplateEntitlement(testContext);

        validateClusterTemplate(testContext, globalDefaultClusterTemplate(), "Template must be global.");
        validateBlueprint(testContext, globalDefaultBlueprint(), "Blueprint must be global.");
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is revoked",
            when = "get one cluster template and one blueprint",
            then = "all of them are account default")
    public void testGetClusterTemplateWhenGlobalDefaultDisabled(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        revokeEntitlement(testContext, Entitlement.CDP_GLOBAL_DEFAULT_TEMPLATE);

        validateAllClusterTemplates(testContext, accountDefaultClusterTemplate(testContext));
        validateClusterTemplate(testContext, accountDefaultClusterTemplate(testContext), "Template must be account default.");
        validateBlueprint(testContext, accountDefaultBlueprint(testContext), "Blueprint must be account default.");
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement and lake house is revoked",
            when = "get one cluster template and one blueprint",
            then = "all of them are account default and lake house is not present")
    public void testLakeHousIsFilteredOutWhenGlobalDefaultDisabled(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        revokeEntitlement(testContext, Entitlement.CDP_GLOBAL_DEFAULT_TEMPLATE);
        revokeEntitlement(testContext, Entitlement.CDP_LAKEHOUSE_OPTIMIZER_ENABLED);

        validateAllClusterTemplates(testContext, accountDefaultClusterTemplate(testContext)
                .and(template -> !template.getName().contains("Lakehouse Optimizer")));
        validateLakeHouseClusterTemplateNotFound(testContext);
        validateLakeHouseBlueprintNotFound(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "the global template entitlement is granted and lake house is revoked",
            when = "get one cluster template and one blueprint",
            then = "all of them are global default and lake house is not present")
    public void testLakeHousIsFilteredOutWhenGlobalDefaultEnabled(MockedTestContext testContext) {
        createDefaultEnvironment(testContext);

        grantGlobalTemplateEntitlement(testContext);
        revokeEntitlement(testContext, Entitlement.CDP_LAKEHOUSE_OPTIMIZER_ENABLED);

        validateAllClusterTemplates(testContext, globalDefaultClusterTemplate()
                .and(template -> !template.getName().contains("Lakehouse Optimizer")));
        validateLakeHouseClusterTemplateNotFound(testContext);
        validateLakeHouseBlueprintNotFound(testContext);
    }

    private int getNumberOfTemplates(MockedTestContext testContext) {
        AtomicInteger result = new AtomicInteger();
        testContext.given(ClusterTemplateTestDto.class)
                .when(clusterTemplateTestClient.listV4())
                .then((testContext1, testDto, client) -> {
                    result.set(testDto.getResponses().size());
                    return testDto;
                })
                .validate();
        return result.get();
    }

    private void createDefaultClusterTemplate(MockedTestContext testContext) {
        testContext
                .given("placementSettings", PlacementSettingsTestDto.class)
                .withRegion(MockCloudProvider.LONDON)
                .given("stackTemplate", StackTemplateTestDto.class)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .withPlacement("placementSettings")
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .when(clusterTemplateTestClient.createV4())
                .validate();
    }

    private String newAccount() {
        return String.format(ACCOUNT_TEMPLATE, UUID.randomUUID());
    }

    private void grantGlobalTemplateEntitlement(MockedTestContext testContext) {
        testContext.given(UmsTestDto.class)
                .when(umsTestClient.grantEntitlement(testContext.getActingUserCrn().getAccountId(), Entitlement.CDP_GLOBAL_DEFAULT_TEMPLATE.name(),
                        WAIT_ENTITLEMENT_SECONDS));
    }

    private void revokeEntitlement(MockedTestContext testContext, Entitlement entitlement) {
        testContext.given(UmsTestDto.class)
                .when(umsTestClient.revokeEntitlement(testContext.getActingUserCrn().getAccountId(), entitlement.name(),
                        WAIT_ENTITLEMENT_SECONDS));
    }

    private void validateAllClusterTemplates(MockedTestContext testContext, Predicate<ClusterTemplateV4Response> condition) {
        testContext
                .given(ClusterTemplateTestDto.class)
                .when(clusterTemplateTestClient.listV4())
                .then((testContext1, testDto, client) -> {
                    try {
                        List<ClusterTemplateV4Response> wrongClusterDefinitions = testDto
                                .getResponses()
                                .stream()
                                .filter(template -> condition.negate().test(template))
                                .toList();
                        assertTrue(wrongClusterDefinitions.isEmpty(), () ->
                                String.format("Following cluster definitions are violating condition: %s",
                                        wrongClusterDefinitions.stream().map(ClusterTemplateV4Response::getCrn).toList()));
                    } catch (Exception e) {
                        throw new TestFailException(String.format("Failed to validate cluster definitions: %s", e.getMessage()), e);
                    }
                    return testDto;
                })
                .validate();
    }

    private void validateClusterTemplate(MockedTestContext testContext, Predicate<ClusterTemplateV4Response> condition, String errorMessage) {
        testContext.given(ClusterTemplateTestDto.class)
                .withName(CLUSTER_TEMPLATE_NAME)
                .when(clusterTemplateTestClient.getV4())
                .then((testContext1, testDto, client) -> {
                    try {
                        assertTrue(condition.test(testDto.getResponse()), errorMessage);
                    } catch (Exception e) {
                        throw new TestFailException(String.format("Failed to validate cluster template: %s", e.getMessage()), e);
                    }
                    return testDto;
                })
                .validate();
    }

    private void validateLakeHouseClusterTemplateNotFound(MockedTestContext testContext) {
        testContext.given(ClusterTemplateTestDto.class)
                .withName(LAKE_HOUSE_CLUSTER_TEMPLATE_NAME)
                .whenException(clusterTemplateTestClient.getV4(), NotFoundException.class)
                .validate();
    }

    private void validateAllBlueprints(MockedTestContext testContext, Predicate<String> condition) {
        testContext.init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4())
                .then((tc, testDto, client) -> {
                    try {
                        List<BlueprintV4ViewResponse> wrongClusterDefinitions = testDto
                                .getViewResponses()
                                .stream()
                                .filter(blueprint -> condition.negate().test(blueprint.getCrn()))
                                .toList();
                        assertTrue(wrongClusterDefinitions.isEmpty(), () ->
                                String.format("Following blueprints are violating condition: %s",
                                        wrongClusterDefinitions.stream().map(BlueprintV4ViewResponse::getCrn).toList()));
                    } catch (Exception e) {
                        throw new TestFailException(String.format("Failed to validate blueprints: %s", e.getMessage()), e);
                    }
                    return testDto;
                })
                .validate();
    }

    private void validateUsedBlueprint(MockedTestContext testContext, Predicate<String> condition) {
        testContext
                .given(DistroXTestDto.class)
                .refresh()
                .then((tc, testDto, client) -> {
                    try {
                        String blueprintCrn = testDto.getResponse().getCluster().getBlueprint().getCrn();
                        assertTrue(condition.test(blueprintCrn));
                    } catch (Exception e) {
                        throw new TestFailException(String.format("Failed to validate blueprint crn: %s", e.getMessage()), e);
                    }
                    return testDto;
                })
                .validate();
    }

    private void validateBlueprint(MockedTestContext testContext, Predicate<String> condition, String errorMessage) {
        testContext
                .given(BlueprintTestDto.class)
                .withName(BLUEPRINT_NAME)
                .when(blueprintTestClient.getV4())
                .then((tc, testDto, client) -> {
                    try {
                        assertTrue(condition.test(testDto.getResponse().getCrn()), errorMessage);
                    } catch (Exception e) {
                        throw new TestFailException(String.format("Failed to validate blueprint: %s", e.getMessage()), e);
                    }
                    return testDto;
                })
                .validate();
    }

    private void validateLakeHouseBlueprintNotFound(MockedTestContext testContext) {
        testContext
                .given(BlueprintTestDto.class)
                .withName(LAKE_HOUSE_BLUEPRINT_NAME)
                .whenException(blueprintTestClient.getV4(), NotFoundException.class)
                .validate();
    }

    private Predicate<ClusterTemplateV4Response> globalDefaultClusterTemplate() {
        return template -> template.getCrn().startsWith(GLOBAL_CLUSTER_DEF_CRN_PREFIX);
    }

    private Predicate<ClusterTemplateV4Response> accountDefaultClusterTemplate(MockedTestContext testContext) {
        return template -> template.getCrn().startsWith(String.format(ACCOUNT_DEFAULT_CLUSTER_DEF_CRN,
                testContext.getActingUserCrn().getAccountId()));
    }

    private Predicate<String> globalDefaultBlueprint() {
        return crn -> crn.startsWith(GLOBAL_BLUEPRINT_CRN_PREFIX);
    }

    private Predicate<String> accountDefaultBlueprint(MockedTestContext testContext) {
        return crn -> crn.startsWith(String.format(ACCOUNT_DEFAULT_BLUEPRINT_CRN, testContext.getActingUserCrn().getAccountId()));
    }

    private Assertion<DistroXTestDto, CloudbreakClient> setRepairableInstances() {
        return (testContext1, testDto, client) -> {
            List<String> workerInstanceIds = distroxUtil.getInstanceIds(testDto, client, "worker");
            List<String> computeInstanceIds = distroxUtil.getInstanceIds(testDto, client, "compute");
            testDto.setRepairableInstanceIds(Lists.merge(workerInstanceIds, computeInstanceIds));
            return testDto;
        };
    }

    private void repairDataHub(MockedTestContext testContext) {
        testContext.given(DistroXTestDto.class)
                .then(setRepairableInstances())
                .when(distroXClient.repairInstances())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .valid();
    }
}
