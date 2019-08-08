package com.sequenceiq.it.cloudbreak.testcase.mock.api;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

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
                .when(stackTestClient.getV4(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY).withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateABlueprintAndGetOtherUser(TestContext testContext) {
        testContext
                .given(BlueprintTestDto.class)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .when(blueprintTestClient.getV4(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY).withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateARecipeAndGetOtherUser(TestContext testContext) {
        testContext
                .given(RecipeTestDto.class)
                .when(recipeTestClient.createV4())
                .when(recipeTestClient.getV4(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY).withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnLdapAndGetOtherUser(TestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .when(ldapTestClient.createV1())
                .when(ldapTestClient.describeV1(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY).withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnImageCatalogWithImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.createV4())
                .when(imageCatalogTestClient.getImagesByNameV4(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY)
                        .withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAnImageCatalogWithoutImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.createV4())
                .when(imageCatalogTestClient.getV4(Boolean.FALSE), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY)
                        .withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateAProxyConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .when(proxyTestClient.create())
                .when(proxyTestClient.get(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY).withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    public void testCreateKerberosConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(KerberosTestDto.class)
                .when(kerberosTestClient.createV1())
                .when(kerberosTestClient.describeV1(), RunningParameter.key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_ACCESS_KEY).withLogError(false))
                .expect(NotFoundException.class, RunningParameter.key(FORBIDDEN_KEY))
                .validate();
    }
}
