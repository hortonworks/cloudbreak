package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalog;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.KerberosPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.Recipe;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeEntity;

public class WorkspaceTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceTest.class);

    private static final String DATA_PROVIDER = "testContext";

    private static final String FORBIDDEN_KEY = "forbiddenGetByName";

    private static final String BLUEPRINT_TEXT = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDF\",\"stack_version\":\"3.2\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\","
            + "\"configurations\":[],\"components\":[{\"name\":\"METRICS_MONITOR\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":\"ZOOKEEPER_CLIENT\"}],"
            + "\"cardinality\":\"1\"}]}";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateAStackAndGetOtherUser(TestContext testContext) {
        testContext
                .given(StackEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateACredentialAndGetOtherUser(TestContext testContext) {
        testContext
                .given(CredentialEntity.class)
                .when(Credential::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateABlueprintAndGetOtherUser(TestContext testContext) {
        testContext
                .given(BlueprintEntity.class).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV4())
                .when(Blueprint::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateARecipeAndGetOtherUser(TestContext testContext) {
        testContext
                .given(RecipeEntity.class)
                .when(Recipe.postV4())
                .when(Recipe::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateAnLdapAndGetOtherUser(TestContext testContext) {
        testContext
                .given(LdapConfigEntity.class)
                .when(LdapConfig.postV2())
                .when(LdapConfig::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateAnImageCatalogWithImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogEntity.class)
                .when(ImageCatalog.postV2())
                .when(ImageCatalog::getByNameAndImages, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateAnImageCatalogWithoutImagesAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ImageCatalogEntity.class)
                .when(ImageCatalog.postV2())
                .when(ImageCatalog::getByNameWithoutImages, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateAProxyConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(ProxyConfigEntity.class)
                .when(ProxyConfig.postV4())
                .when(ProxyConfig::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER, enabled = false)
    public void testCreateKerberosConfigAndGetOtherUser(TestContext testContext) {
        testContext
                .given(KerberosEntity.class)
                .when(KerberosPostAction.create())
                .when(this::getByName, key(FORBIDDEN_KEY).withWho(CloudbreakTest.SECONDARY_REFRESH_TOKEN).withLogError(false))
                .expect(ForbiddenException.class, key(FORBIDDEN_KEY))
                .validate();
    }

    private KerberosEntity getByName(TestContext testContext, KerberosEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().kerberosConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}