package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class WorkspaceTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceTest.class);

    private static final String FORBIDDEN_KEY = "forbiddenGetByName";

    private static final String BLUEPRINT_TEXT = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDF\",\"stack_version\":\"3.2\"},"
            + "\"settings\":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\""
            + ":\"master\",\"configurations\":[],\"components\":[{\"name\":\"METRICS_MONITOR\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":\"ZOOKEEPER_CLIENT\""
            + "}],\"cardinality\":\"1\"}]}";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAStackAndGetOtherUser(MockedTestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateACredentialAndGetOtherUser(TestContext testContext) {
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateABlueprintAndGetOtherUser(TestContext testContext) {
        testContext
                .given(BlueprintTestDto.class)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .when(blueprintTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateARecipeAndGetOtherUser(TestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnLdapAndGetOtherUser(TestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .when(ldapTestClient.createV4())
                .when(ldapTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnImageCatalogWithImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.createV4())
                .when(imageCatalogTestClient.getImagesByNameV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnImageCatalogWithoutImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.createV4())
                .when(imageCatalogTestClient.getV4(Boolean.FALSE), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAProxyConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .when(proxyTestClient.createV4())
                .when(proxyTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateKerberosConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(KerberosTestDto.class)
                .when(kerberosTestClient.createV4())
                .when(kerberosTestClient.getV4(), key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
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