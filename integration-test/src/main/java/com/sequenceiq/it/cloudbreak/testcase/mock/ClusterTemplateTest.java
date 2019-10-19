package com.sequenceiq.it.cloudbreak.testcase.mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.clustertemplate.ClusterTemplateTestAssertion;
import com.sequenceiq.it.cloudbreak.client.ClusterTemplateTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.mock.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.DistroXTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTemplateTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class ClusterTemplateTest extends AbstractIntegrationTest {

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String SPECIAL_CT_NAME = "@#$|:&* ABC";

    private static final String ILLEGAL_CT_NAME = "Illegal template name ;";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private ClusterTemplateTestClient clusterTemplateTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a valid cluster template create request is sent",
            then = "the cluster template is created and can be deleted"
    )
    public void testClusterTemplateCreateAndGetAndDelete(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();
        String stackTemplate = resourcePropertyProvider().getName();

        testContext
                .given(stackTemplate, StackTemplateTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .when(clusterTemplateTestClient.getV4(), RunningParameter.key(generatedKey))
                .then(ClusterTemplateTestAssertion.getResponse(), RunningParameter.key(generatedKey))
                .then(ClusterTemplateTestAssertion.checkStackTemplateAfterClusterTemplateCreation(), RunningParameter.key(generatedKey))
                .when(clusterTemplateTestClient.deleteV4(), RunningParameter.key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared cluster template",
            when = "a stack is created from the prepared cluster template",
            then = "the stack starts properly and can be deleted"
    )
    public void testLaunchClusterFromTemplate(MockedTestContext testContext) {
        String cm = resourcePropertyProvider().getName();
        String templateName = resourcePropertyProvider().getName();
        String name = resourcePropertyProvider().getName();
        String template = resourcePropertyProvider().getName();
        String distroxTemplate = resourcePropertyProvider().getName();

        testContext
                .given("clusterWithUnPw", ClusterTestDto.class)
                .withUserName("someusername")
                .withPassword("Passw0rd")
                .given(cm, DistroXClouderaManagerTestDto.class)
                .withoutRepository()
                .given(DistroXNetworkTestDto.class)
                .given(ImageCatalogTestDto.class)
                .given(DistroXImageTestDto.class)
                .withImageId(IMAGE_CATALOG_ID)
                .withImageCatalog()
                .given(distroxTemplate, DistroXTemplateTestDto.class)
                .withImage()
                .withMockedGatewayPort()
                .withDefaultThreeInstanceGroups()
                .withCluster("clusterWithUnPw")
                .given(template, ClusterTemplateTestDto.class)
                .withNetwork()
                .withImageSettings()
                .withType(ClusterTemplateV4Type.DATASCIENCE)
                .withDistroXTemplateKey(distroxTemplate)
                .withCM(cm)
                .withName(templateName)
                .when(clusterTemplateTestClient.createV4())
                .given(DistroXTestDto.class)
                .fromClusterDefinition(template)
                .withName(name)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster template create request with null environment name is sent",
            then = "the cluster template is cannot be created"
    )
    public void testCreateClusterTemplateWithoutEnvironmentName(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext.given(DistroXTemplateTestDto.class)
                .withEnvironmentName(null)
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplate()
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey)
                        .withExpectedMessage("The environmentName cannot be null."))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an environment with an attached cluster definition",
            when = "the environment has deleted",
            then = "the related cluster definition should also get deleted"
    )
    public void testEnvironmentDeletionAlsoRemovesTheAttachedClusterDefinitions(MockedTestContext testContext) {
        String templateName = resourcePropertyProvider().getName();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(templateName)
                .withType(ClusterTemplateV4Type.DATASCIENCE)
                .when(clusterTemplateTestClient.createV4())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)
                .given(ClusterTemplateTestDto.class)
                .then((context, dto, client) -> clusterTemplateHasDeletedAlongTheEnvironment(dto, client, templateName))
                .validate();
    }

    /*@Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a prepared cluster template with many properties",
            when = "a stack is created from the prepared cluster template",
            then = "the stack starts properly and can be deleted"
    )
    public void testLaunchClusterFromTemplateWithProperties(MockedTestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .withName("mock-test-ldap")
                .when(ldapTestClient.createV1())

                .given(RecipeTestDto.class)
                .withName("mock-test-recipe")
                .when(recipeTestClient.createV4())

                .given(DatabaseTestDto.class)
                .withName("mock-test-rds")
                .when(new DatabaseCreateIfNotExistsAction())

                .given("mpack", MPackTestDto.class)
                .withName("mock-test-mpack")
                .when(mpackTestClient.createV4())

                .given(StackTemplateTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withEveryProperties()

                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .when(clusterTemplateTestClient.createV4())
                .when(clusterTemplateTestClient.getV4())
                .then(ClusterTemplateTestAssertion.checkStackTemplateAfterClusterTemplateCreationWithProperties())

                .when(clusterTemplateTestClient.launchCluster(StackTemplateTestDto.class))
                .given(StackTemplateTestDto.class)
                .await(STACK_AVAILABLE)

                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .when(clusterTemplateTestClient.deleteCluster(StackTemplateTestDto.class), RunningParameter.force())
                .given(StackTemplateTestDto.class)
                .await(STACK_DELETED, RunningParameter.force())
                .validate();
    }*/

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a create cluster template request is sent with too long description",
            then = "the a cluster template should not be created"
    )
    public void testListDefaultClusterTemplate(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();
        testContext
                .given(ClusterTemplateTestDto.class)
                .when(clusterTemplateTestClient.listV4(), RunningParameter.key(generatedKey))
                .then(this::validateDefaultCount)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster template create request is sent with invalid name",
            then = "the cluster template cannot be created"
    )
    public void testCreateInvalidNameClusterTemplate(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(ILLEGAL_CT_NAME)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey)
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
        String generatedKey = resourcePropertyProvider().getName();
        String stackTemplate = resourcePropertyProvider().getName();
        String name = StringUtils.substring(resourcePropertyProvider().getName(), 0, 40 - SPECIAL_CT_NAME.length()) + SPECIAL_CT_NAME;

        testContext
                .given(stackTemplate, StackTemplateTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withName(name)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .then((testContext1, testDto, client) -> clusterTemplateHasCreatedAndCanBeListed(testDto, client, name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster template create request is sent with a too short name",
            then = "the cluster template cannot be created"
    )
    public void testCreateInvalidShortNameClusterTemplate(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(2))
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey)
                        .withExpectedMessage("The length of the cluster's name has to be in range of 5 to 40")
                )
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster definition create request is sent without blueprint name",
            then = "the cluster definition cannot be created"
    )
    public void testCreateWithoutBlueprintInCluster(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given("dixTemplate", DistroXTemplateTestDto.class)
                .withBlueprintName(null)
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplateKey("dixTemplate")
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster definition create request is sent with an empty blueprint name",
            then = "the cluster definition cannot be created"
    )
    public void testCreateWithEmptyBlueprintInCluster(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given("dixTemplate", DistroXTemplateTestDto.class)
                .withBlueprintName("")
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplateKey("dixTemplate")
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster definition create request is sent with a not existing blueprint name",
            then = "the cluster definition cannot be created"
    )
    public void testCreateWithNotExistingBlueprintInCluster(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given("dixTemplate", DistroXTemplateTestDto.class)
                .withBlueprintName("thisBlueprintDoesNotExistsForSure")
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplateKey("dixTemplate")
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment and cluster template",
            when = "the cluster template create request is sent again",
            then = "a BadRequest should be returned"
    )
    public void testCreateAgainClusterTemplate(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given("placementSettings", PlacementSettingsTestDto.class)
                .withRegion(MockCloudProvider.EUROPE)
                .given("stackTemplate", StackTemplateTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .withPlacement("placementSettings")
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey)
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
        String generatedKey = resourcePropertyProvider().getName();
        String invalidLongDescripton = getLongNameGenerator().stringGenerator(1001);
        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .withDescription(invalidLongDescripton)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey)
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
        String generatedKey = resourcePropertyProvider().getName();

        testContext.given(ClusterTemplateTestDto.class)
                .withDistroXTemplate(null)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey)
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
        String generatedKey1 = resourcePropertyProvider().getName();
        String generatedKey2 = resourcePropertyProvider().getName();

        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(null)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey1))
                .given(ClusterTemplateTestDto.class)
                .withName("")
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey2)
                        .withSkipOnFail(false))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey1).withExpectedMessage("must not be null"))
                .expect(BadRequestException.class, RunningParameter.key(generatedKey2)
                        .withExpectedMessage("The length of the cluster's name has to be in range of 5 to 40"))
                .validate();
    }

    private ClusterTemplateTestDto validateDefaultCount(TestContext tc, ClusterTemplateTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity);
        assertNotNull(entity.getResponses());
        long defaultCount = entity.getResponses().stream().filter(template -> ResourceStatus.DEFAULT.equals(template.getStatus())).count();
        assertEquals("Should have 8 of default cluster templates", 8, defaultCount);
        return entity;
    }

    private ClusterTemplateTestDto clusterTemplateHasDeletedAlongTheEnvironment(ClusterTemplateTestDto entity, CloudbreakClient client, String templateName) {
        client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .list(client.getWorkspaceId())
                .getResponses()
                .stream()
                .filter(response -> templateName.equals(response.getName()))
                .findFirst()
                .ifPresent(response -> {
                    throw new TestFailException("The cluster definition still exists even though the related environment has deleted!");
                });
        return entity;
    }

    private ClusterTemplateTestDto clusterTemplateHasCreatedAndCanBeListed(ClusterTemplateTestDto entity, CloudbreakClient client, String templateName) {
        client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .list(client.getWorkspaceId())
                .getResponses()
                .stream()
                .filter(response -> templateName.equals(response.getName()))
                .findFirst().orElseThrow(() -> new TestFailException("The expected cluster definition does not exist in the list!"));
        return entity;
    }

}
