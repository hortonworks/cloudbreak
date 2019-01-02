package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type.SPARK;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.force;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.Recipe;
import com.sequenceiq.it.cloudbreak.newway.RecipeEntity;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateV4CreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateV4DeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateV4ListAction;
import com.sequenceiq.it.cloudbreak.newway.action.DeleteClusterFromTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.action.LaunchClusterFromTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.action.LdapConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.ManagementPackCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.RdsConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckClusterTemplateGetResponse;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckClusterTemplateType;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckStackTemplateAfterClusterTemplateCreation;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckStackTemplateAfterClusterTemplateCreationWithProperties;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.GeneralSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ManagementPackEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ClusterTemplateTest extends AbstractIntegrationTest {

    public static final String EUROPE = "Europe";

    private static final Set<String> VALID_REGION = Collections.singleton(EUROPE);

    private static final String VALID_LOCATION = "London";

    private static final String SPECIAL_CT_NAME = "@#$|:&* ABC";

    private static final String ILLEGAL_CT_NAME = "Illegal template name ;";

    private static final String INVALID_SHORT_CT_NAME = "";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Method method, Object[] data) {
        TestContext testContext = (TestContext) data[0];

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testClusterTemplateCreateAndGetAndDelete(TestContext testContext) {
        testContext
                .given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("stackTemplate", StackTemplateEntity.class).withGeneralSettings("generalSettings")
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")

                .when(new ClusterTemplateV4CreateAction())
                .when(new ClusterTemplateV4ListAction())
                .then(new CheckClusterTemplateGetResponse())
                .then(new CheckStackTemplateAfterClusterTemplateCreation())
                .capture(ClusterTemplateEntity::count, key("ctSize"))
                .when(new ClusterTemplateV4DeleteAction())
                .capture(ct -> ct.count() - 1, key("ctSize"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testClusterTemplateWithType(TestContext testContext) {
        testContext.given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("stackTemplate", StackTemplateEntity.class).withGeneralSettings("generalSettings")
                .given(ClusterTemplateEntity.class).withType(SPARK).withStackTemplate("stackTemplate")
                .capture(ClusterTemplateEntity::count, key("ctSize"))
                .when(new ClusterTemplateV4CreateAction())
                .verify(ct -> ct.count() - 1, key("ctSize"))
                .when(new ClusterTemplateV4ListAction())
                .then(new CheckClusterTemplateType(SPARK))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testLaunchClusterFromTemplate(TestContext testContext) {
        testContext.given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("placementSettings", PlacementSettingsEntity.class).withRegion(EUROPE)
                .given("stackTemplate", StackTemplateEntity.class).withGeneralSettings("generalSettings").withPlacementSettings("placementSettings")
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateV4CreateAction())
                .when(new LaunchClusterFromTemplateAction("stackTemplate"))
                .await(STACK_AVAILABLE, key("stackTemplate"))
                .when(new DeleteClusterFromTemplateAction("stackTemplate"))
                .await(STACK_DELETED, key("stackTemplate"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateClusterTemplateWithoutEnvironment(TestContext testContext) {
        testContext.given("stackTemplate", StackTemplateEntity.class)
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateV4CreateAction(), key("ENVIRONMENT_NULL"))
                .except(BadRequestException.class, key("ENVIRONMENT_NULL"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testLaunchClusterFromTemplateWithProperties(TestContext testContext) {
        testContext
                .given(LdapConfigEntity.class).withName("mock-test-ldap")
                .when(new LdapConfigCreateIfNotExistsAction())
                .given(RecipeEntity.class).withName("mock-test-recipe")
                .when(Recipe.postV2())
                .given(RdsConfigEntity.class).withName("mock-test-rds")
                .when(new RdsConfigCreateIfNotExistsAction())
                .given("mpack", ManagementPackEntity.class).withName("mock-test-mpack")
                .when(new ManagementPackCreateAction())
                .given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("stackTemplate", StackTemplateEntity.class).withEveryProperties()
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .capture(ClusterTemplateEntity::count, key("ctSize"))
                .when(new ClusterTemplateV4CreateAction())
                .verify(ct -> ct.count() - 1, key("ctSize"))
                .when(new ClusterTemplateV4ListAction())
                .then(new CheckStackTemplateAfterClusterTemplateCreationWithProperties())
                .when(new LaunchClusterFromTemplateAction("stackTemplate"))
                .await(STACK_AVAILABLE, key("stackTemplate"))
                .when(new DeleteClusterFromTemplateAction("stackTemplate"), force())
                .await(STACK_DELETED, key("stackTemplate").withSkipOnFail(false))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateInvalidNameClusterTemplate(TestContext testContext) {
        testContext.given(ClusterTemplateEntity.class).withName(ILLEGAL_CT_NAME)
                .when(new ClusterTemplateV4CreateAction(), key("illegalCtName"))
                .except(BadRequestException.class, key("illegalCtName"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateSpecialNameClusterTemplate(TestContext testContext) {
        testContext.given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("placementSettings", PlacementSettingsEntity.class).withRegion(EUROPE)
                .given("stackTemplate", StackTemplateEntity.class).withGeneralSettings("generalSettings").withPlacementSettings("placementSettings")
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate").withName(SPECIAL_CT_NAME)
                .when(new ClusterTemplateV4CreateAction())
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateInvalidShortNameClusterTemplate(TestContext testContext) {
        testContext.given(ClusterTemplateEntity.class).withName("sh")
                .when(new ClusterTemplateV4CreateAction(), key("illegalCtName"))
                .except(BadRequestException.class, key("illegalCtName"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateAgainClusterTemplate(TestContext testContext) {
        testContext.given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given(ClusterTemplateEntity.class).given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("placementSettings", PlacementSettingsEntity.class).withRegion(EUROPE)
                .given("stackTemplate", StackTemplateEntity.class).withGeneralSettings("generalSettings").withPlacementSettings("placementSettings")
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateV4CreateAction())
                .when(new ClusterTemplateV4CreateAction(), key("againCtName"))
                .except(BadRequestException.class, key("againCtName"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateLongDescriptionClusterTemplate(TestContext testContext) {
        String invalidLongDescripton = longStringGeneratorUtil.stringGenerator(1001);
        testContext.given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given(ClusterTemplateEntity.class).withDescription(invalidLongDescripton)
                .when(new ClusterTemplateV4CreateAction(), key("longCtDescription"))
                .except(BadRequestException.class, key("longCtDescription"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEmptyStackTemplateClusterTemplateException(TestContext testContext) {
        testContext.given(ClusterTemplateEntity.class).withoutStackTemplate()
                .when(new ClusterTemplateV4CreateAction(), key("emptyStack"))
                .except(BadRequestException.class, key("emptyStack"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateEmptyClusterTemplateNameException(TestContext testContext) {
        testContext
                .given(ClusterTemplateEntity.class).withName(null)
                .when(new ClusterTemplateV4CreateAction(), key("nullTemplateName"))
                .given(ClusterTemplateEntity.class).withName("")
                .when(new ClusterTemplateV4CreateAction(), key("emptyTemplateName").withSkipOnFail(false))
                .except(BadRequestException.class, key("nullTemplateName"))
                .except(BadRequestException.class, key("emptyTemplateName"))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
