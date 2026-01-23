package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.audit.ClusterTemplateAuditGrpcServiceAssertion;
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
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class ClusterTemplateTest extends AbstractMockTest {

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

    @Inject
    private ClusterTemplateAuditGrpcServiceAssertion clusterTemplateAuditGrpcServiceAssertion;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a valid cluster template create request is sent",
            then = "the cluster template is created and can be deleted"
    )
    public void testClusterTemplateCreateAndGetAndDelete(MockedTestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();
        String stackTemplate = resourcePropertyProvider().getName();

        testContext
                .given(stackTemplate, StackTemplateTestDto.class)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .withAutTls(Boolean.TRUE)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .when(clusterTemplateTestClient.getV4(), RunningParameter.key(generatedKey))
                .then(ClusterTemplateTestAssertion.getResponse(), RunningParameter.key(generatedKey))
                .then(ClusterTemplateTestAssertion.checkStackTemplateAfterClusterTemplateCreation(), RunningParameter.key(generatedKey))
                .then(ClusterTemplateTestAssertion.assertAutoTlsTrue())
                .when(clusterTemplateTestClient.deleteV4(), RunningParameter.key(generatedKey))
                .then(clusterTemplateAuditGrpcServiceAssertion::create)
                .then(clusterTemplateAuditGrpcServiceAssertion::delete)
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

        createDatalake(testContext);

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
    public void testCreateClusterTemplateWithoutEnvironmentName(MockedTestContext testContext) {
        testContext.given(DistroXTemplateTestDto.class)
                .withEnvironmentName(null)
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplate()
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("The environmentName cannot be null."))
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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a cluster template list request",
            when = " list requested",
            then = " the response has defined number of cluster templates"
    )
    public void testListDefaultClusterTemplate(MockedTestContext testContext) {
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
    public void testCreateInvalidNameClusterTemplate(MockedTestContext testContext) {
        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(ILLEGAL_CT_NAME)
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("Name should not contain semicolon," +
                        " forward slash or percentage characters"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster template create request is sent with a special name",
            then = "the cluster template creation should be successful"
    )
    public void testCreateSpecialNameClusterTemplate(MockedTestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();
        String stackTemplate = resourcePropertyProvider().getName();
        String name = StringUtils.substring(resourcePropertyProvider().getName(), 0, 40 - SPECIAL_CT_NAME.length()) + SPECIAL_CT_NAME;

        testContext
                .given(stackTemplate, StackTemplateTestDto.class)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withName(name)
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .when(clusterTemplateTestClient.listV4())
                .then((testContext1, testDto, client) -> clusterTemplateHasCreatedAndCanBeListed(testDto, client, name))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster template create request is sent with a too short name",
            then = "the cluster template cannot be created"
    )
    public void testCreateInvalidShortNameClusterTemplate(MockedTestContext testContext) {
        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(getLongNameGenerator().stringGenerator(2))
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("The length of name has to be in range" +
                        " of 5 to 40"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster definition create request is sent without blueprint name",
            then = "the cluster definition cannot be created"
    )
    public void testCreateWithoutBlueprintInCluster(MockedTestContext testContext) {
        testContext
                .given("dixTemplate", DistroXTemplateTestDto.class)
                .withBlueprintName(null)
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplateKey("dixTemplate")
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster definition create request is sent with an empty blueprint name",
            then = "the cluster definition cannot be created"
    )
    public void testCreateWithEmptyBlueprintInCluster(MockedTestContext testContext) {
        testContext
                .given("dixTemplate", DistroXTemplateTestDto.class)
                .withBlueprintName("")
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplateKey("dixTemplate")
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster definition create request is sent with a not existing blueprint name",
            then = "the cluster definition cannot be created"
    )
    public void testCreateWithNotExistingBlueprintInCluster(MockedTestContext testContext) {
        testContext
                .given("dixTemplate", DistroXTemplateTestDto.class)
                .withBlueprintName("thisBlueprintDoesNotExistsForSure")
                .given(ClusterTemplateTestDto.class)
                .withDistroXTemplateKey("dixTemplate")
                .whenException(clusterTemplateTestClient.createV4(), NotFoundException.class,
                        expectedMessage("No cluster template found with name 'thisBlueprintDoesNotExistsForSure'"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment and cluster template",
            when = "the cluster template create request is sent again",
            then = "a BadRequest should be returned"
    )
    public void testCreateAgainClusterTemplate(MockedTestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();

        testContext
                .given("placementSettings", PlacementSettingsTestDto.class)
                .withRegion(MockCloudProvider.LONDON)
                .given("stackTemplate", StackTemplateTestDto.class)
                .withEnvironmentClass(EnvironmentTestDto.class)
                .withPlacement("placementSettings")
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .when(clusterTemplateTestClient.createV4(), RunningParameter.key(generatedKey))
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class,
                        expectedMessage("^Cluster definition already exists with name.*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a create cluster template request is sent with too long description",
            then = "the a cluster template should not be created"
    )
    public void testCreateLongDescriptionClusterTemplate(MockedTestContext testContext) {
        String invalidLongDescription = getLongNameGenerator().stringGenerator(1001);
        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(resourcePropertyProvider().getName())
                .withDescription(invalidLongDescription)
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("size must be between 0 and 1000"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster template create request without stack template is sent",
            then = "the a cluster template should not be created"
    )
    public void testCreateEmptyStackTemplateClusterTemplateException(MockedTestContext testContext) {
        testContext.given(ClusterTemplateTestDto.class)
                .withDistroXTemplate(null)
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("must not be null"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a prepared environment",
            when = "a cluster tempalte create request with null name is sent",
            then = "the a cluster template should not be created"
    )
    public void testCreateEmptyClusterTemplateNameException(MockedTestContext testContext) {
        testContext
                .given(ClusterTemplateTestDto.class)
                .withName(null)
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("must not be null").withSkipOnFail(false))
                .given(ClusterTemplateTestDto.class)
                .withName("")
                .whenException(clusterTemplateTestClient.createV4(), BadRequestException.class, expectedMessage("The length of name has to be in range" +
                        " of 5 to 40"))
                .validate();
    }

    private ClusterTemplateTestDto validateDefaultCount(TestContext tc, ClusterTemplateTestDto entity, CloudbreakClient cc) {
        try {
            assertNotNull(entity);
            assertNotNull(entity.getResponses());
            long defaultCount = entity.getResponses().stream().filter(template -> ResourceStatus.DEFAULT.equals(template.getStatus())).count();
            long expectedCount = 773;
            assertEquals(expectedCount, defaultCount, "Should have " + expectedCount + " of default cluster templates.");
        } catch (Exception e) {
            throw new TestFailException(String.format("Failed to validate default count of cluster templates: %s", e.getMessage()), e);
        }
        return entity;
    }

    private ClusterTemplateTestDto clusterTemplateHasDeletedAlongTheEnvironment(ClusterTemplateTestDto entity, CloudbreakClient client, String templateName) {
        client.getDefaultClient(entity.getTestContext())
                .clusterTemplateV4EndPoint()
                .listByEnv(client.getWorkspaceId(), entity.getResponse().getEnvironmentCrn())
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
        entity.getResponses()
                .stream()
                .filter(response -> templateName.equals(response.getName()))
                .findFirst().orElseThrow(() -> new TestFailException("The expected cluster definition does not exist in the list!"));
        return entity;
    }

}
