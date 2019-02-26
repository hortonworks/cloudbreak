package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogGetByNameAction;
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostAction;
import com.sequenceiq.it.cloudbreak.newway.action.kerberos.KerberosTestAction;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.Recipe;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class WorkspaceTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceTest.class);

    private static final String FORBIDDEN_KEY = "forbiddenGetByName";

    private static final String CLUSTER_DEFINITION_TEXT = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDF\",\"stack_version\":\"3.2\"},"
            + "\"settings\":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\""
            + ":\"master\",\"configurations\":[],\"components\":[{\"name\":\"METRICS_MONITOR\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":\"ZOOKEEPER_CLIENT\""
            + "}],\"cardinality\":\"1\"}]}";

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAStackAndGetOtherUser(MockedTestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateACredentialAndGetOtherUser(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(Credential::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAClusterDefinitionAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ClusterDefinitionTestDto.class).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .when(ClusterDefinitionTestAction::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateARecipeAndGetOtherUser(TestContext testContext) {
        testContext
                .given(RecipeEntity.class)
                .when(Recipe.postV4())
                .when(Recipe::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnLdapAndGetOtherUser(TestContext testContext) {
        testContext
                .given(LdapConfigTestDto.class)
                .when(ldapConfigTestClient.post())
                .when(ldapConfigTestClient::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnImageCatalogWithImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetByNameAction(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnImageCatalogWithoutImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogPostAction())
                .when(new ImageCatalogGetByNameAction(Boolean.FALSE), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAProxyConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ProxyConfigEntity.class)
                .when(ProxyConfig.postV4())
                .when(ProxyConfig::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateKerberosConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(KerberosTestDto.class)
                .when(KerberosTestAction::post)
                .when(this::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    private KerberosTestDto getByName(TestContext testContext, KerberosTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().kerberosConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}