package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type.SPARK;
import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.mock.MockCloudProvider.LONDON;
import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.mock.MockCloudProvider.VALID_REGION;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.force;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.v4.database.DatabaseCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckClusterTemplateGetResponse;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckClusterTemplateType;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckStackTemplateAfterClusterTemplateCreation;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckStackTemplateAfterClusterTemplateCreationWithProperties;
import com.sequenceiq.it.cloudbreak.newway.client.ClusterTemplateTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.MpackTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.mock.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.EnvironmentSettingsV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ClusterTemplateTest extends AbstractIntegrationTest {

    private static final String SPECIAL_CT_NAME = "@#$|:&* ABC";

    private static final String ILLEGAL_CT_NAME = "Illegal template name ;";

    private static final String INVALID_SHORT_CT_NAME = "";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private ClusterTemplateTestClient clusterTemplateTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private MpackTestClient mpackTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @BeforeMethod
    public void beforeMethod(Method method, Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a valid cluster template create request is sent",
            then = "the cluster template is created and can be deleted"
    )
    public void testClusterTemplateCreateAndGetAndDelete(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String stackTemplate = getNameGenerator().getRandomNameForResource();

        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(stackTemplate, StackTemplateEntity.class)
                .withEnvironment(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate(stackTemplate)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .when(clusterTemplateTestClient.getV4(), key(generatedKey))
                .then(new CheckClusterTemplateGetResponse(), key(generatedKey))
                .then(new CheckStackTemplateAfterClusterTemplateCreation(), key(generatedKey))
                .when(clusterTemplateTestClient.deleteV4(), key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a valid cluster template create request with spark type is sent",
            then = "the new cluster template with spark type is listed in the list cluster templates response"
    )
    public void testClusterTemplateWithType(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String environment = getNameGenerator().getRandomNameForResource();
        String stackTemplate = getNameGenerator().getRandomNameForResource();

        testContext
                .given(environment, EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(LONDON)
                .when(environmentTestClient.createV4())
                .given(stackTemplate, StackTemplateEntity.class)
                .withEnvironmentKey(environment)
                .given(ClusterTemplateTestDto.class)
                .withType(SPARK)
                .withStackTemplate(stackTemplate)
                .capture(ClusterTemplateTestDto::count, key(generatedKey))
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .when(clusterTemplateTestClient.listV4(), key(generatedKey))
                .then(new CheckClusterTemplateType(SPARK), key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared cluster template",
            when = "a stack is created from the prepared cluster template",
            then = "the stack starts properly and can be deleted"
    )
    public void testLaunchClusterFromTemplate(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String environment = getNameGenerator().getRandomNameForResource();

        testContext
                .given(environment, EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(LONDON)
                .when(environmentTestClient.createV4(), key(generatedKey))
                .given(generatedKey, StackTemplateEntity.class)
                .withEnvironmentKey(environment)
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate(generatedKey)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .when(clusterTemplateTestClient.launchCluster(generatedKey), key(generatedKey))
                .await(STACK_AVAILABLE, key(generatedKey))
                .when(clusterTemplateTestClient.deleteCluster(generatedKey), key(generatedKey))
                .await(STACK_DELETED, key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster template create request with missing environment is sent",
            then = "the cluster template is cannot be created"
    )
    public void testCreateClusterTemplateWithoutEnvironment(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String stackTemplate = getNameGenerator().getRandomNameForResource();

        testContext
                .given(stackTemplate, StackTemplateEntity.class)
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate(stackTemplate)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("The environment name cannot be null."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster template create request with null environment name is sent",
            then = "the cluster template is cannot be created"
    )
    public void testCreateClusterTemplateWithoutEnvironmentName(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String stackTemplate = getNameGenerator().getRandomNameForResource();

        testContext.given(EnvironmentSettingsV4Entity.class)
                .withName(null)
                .given(stackTemplate, StackTemplateEntity.class)
                .withEnvironmentSettings()
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate(stackTemplate)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("The environment name cannot be null."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared cluster template with many properties",
            when = "a stack is created from the prepared cluster template",
            then = "the stack starts properly and can be deleted"
    )
    public void testLaunchClusterFromTemplateWithProperties(MockedTestContext testContext) {
        testContext.getModel().getAmbariMock().putConfigureLdap();
        testContext.getModel().getAmbariMock().postSyncLdap();
        testContext.getModel().getAmbariMock().putConfigureSso();
        testContext
                .given(LdapTestDto.class)
                .withName("mock-test-ldap")
                .when(ldapTestClient.createV4())
                .given(RecipeTestDto.class)
                .withName("mock-test-recipe")
                .when(recipeTestClient.createV4())
                .given(DatabaseTestDto.class)
                .withName("mock-test-rds")
                .when(new DatabaseCreateIfNotExistsAction())
                .given("mpack", MPackTestDto.class).withName("mock-test-mpack")
                .when(mpackTestClient.createV4())
                .given("environment", EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(LONDON)
                .when(environmentTestClient.createV4())
                .given("stackTemplate", StackTemplateEntity.class)
                .withEnvironmentKey("environment")
                .withEveryProperties()
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate("stackTemplate")
                .when(clusterTemplateTestClient.createV4())
                .when(clusterTemplateTestClient.getV4())
                .then(new CheckStackTemplateAfterClusterTemplateCreationWithProperties())
                .when(clusterTemplateTestClient.launchCluster("stackTemplate"))
                .await(STACK_AVAILABLE, key("stackTemplate"))
                .when(clusterTemplateTestClient.deleteCluster("stackTemplate"), force())
                .await(STACK_DELETED, key("stackTemplate").withSkipOnFail(false))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster template create request is sent with invalid name",
            then = "the cluster template cannot be created"
    )
    public void testCreateInvalidNameClusterTemplate(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(ILLEGAL_CT_NAME)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("The length of the cluster template's name has to be in range of 1 to 100 and should not contain semicolon"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster template create request is sent with a special name",
            then = "the cluster template creation should be successful"
    )
    public void testCreateSpecialNameClusterTemplate(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String stackTemplate = getNameGenerator().getRandomNameForResource();
        String name = StringUtils.substring(getNameGenerator().getRandomNameForResource(), 0, 40 - SPECIAL_CT_NAME.length()) + SPECIAL_CT_NAME;

        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4(), key(generatedKey))
                .given(stackTemplate, StackTemplateEntity.class)
                .withEnvironment(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate(stackTemplate)
                .withName(name)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster template create request is sent with a too short name",
            then = "the cluster template cannot be created"
    )
    public void testCreateInvalidShortNameClusterTemplate(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(2))
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("The length of the cluster's name has to be in range of 5 to 40")
                )
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment and cluster template",
            when = "the cluster template create request is sent again",
            then = "a BadRequest should be returned"
    )
    public void testCreateAgainClusterTemplate(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext
                .given("environment", EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(LONDON)
                .when(environmentTestClient.createV4(), key(generatedKey))
                .given("placementSettings", PlacementSettingsEntity.class)
                .withRegion(MockCloudProvider.EUROPE)
                .given("stackTemplate", StackTemplateEntity.class)
                .withEnvironmentKey("environment")
                .withPlacement("placementSettings")
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate("stackTemplate")
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("^clustertemplate already exists with name.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a create cluster template request is sent with too long description",
            then = "the a cluster template should not be created"
    )
    public void testCreateLongDescriptionClusterTemplate(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String environment = getNameGenerator().getRandomNameForResource();
        String invalidLongDescripton = getLongNameGenerator().stringGenerator(1001);
        testContext
                .given(environment, EnvironmentTestDto.class)
                .withRegions(VALID_REGION)
                .withLocation(LONDON)
                .when(environmentTestClient.createV4(), key(generatedKey))
                .given(ClusterTemplateTestDto.class)
                .withDescription(invalidLongDescripton)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("size must be between 0 and 1000"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster template create request without stack template is sent",
            then = "the a cluster template should not be created"
    )
    public void testCreateEmptyStackTemplateClusterTemplateException(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();

        testContext.given(ClusterTemplateTestDto.class)
                .withoutStackTemplate()
                .when(clusterTemplateTestClient.createV4(), key(generatedKey))
                .expect(BadRequestException.class, key(generatedKey)
                        .withExpectedMessage("must not be null"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster tempalte create request with null name is sent",
            then = "the a cluster template should not be created"
    )
    public void testCreateEmptyClusterTemplateNameException(TestContext testContext) {
        String generatedKey1 = getNameGenerator().getRandomNameForResource();
        String generatedKey2 = getNameGenerator().getRandomNameForResource();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(null)
                .when(clusterTemplateTestClient.createV4(), key(generatedKey1))
                .given(ClusterTemplateTestDto.class)
                .withName("")
                .when(clusterTemplateTestClient.createV4(), key(generatedKey2)
                        .withSkipOnFail(false))
                .expect(BadRequestException.class, key(generatedKey1).withExpectedMessage("must not be null"))
                .expect(BadRequestException.class, key(generatedKey2)
                        .withExpectedMessage("The length of the cluster's name has to be in range of 5 to 40"))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
