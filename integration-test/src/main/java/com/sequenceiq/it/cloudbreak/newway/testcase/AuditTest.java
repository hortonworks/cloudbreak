package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.audit.AuditTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.clustertemplate.ClusterTemplateV4CreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.credential.CredentialTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.kerberos.KerberosTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.kubernetes.KubernetesTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.mpack.MpackTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.audit.AuditTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.entity.blueprint.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.Recipe;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

// TODO image catalog test missing because the image catalog DTO creation is in progress
public class AuditTest extends AbstractIntegrationTest {

    private static final String VALID_BP = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"}],\"cardinality\":\"1"
            + "\"}]}";

    @Inject
    private LdapConfigTestClient ldapConfigTestClient;

    @Override
    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForRecipeAndThenValidate(TestContext testContext) {
        String recipeName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(RecipeEntity.class)
                .withName(recipeName)
                .when(Recipe.postV4(), key(recipeName))
                .select(recipe -> recipe.getResponse().getId(), key(recipeName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(recipeName)
                .withResourceType("recipes")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForKubernetesAndThenValidate(TestContext testContext) {
        String kubernetesName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(KubernetesTestAction::create, key(kubernetesName))
                .select(kubernetes -> kubernetes.getResponse().getId(), key(kubernetesName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(kubernetesName)
                .withResourceType("kubernetes")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForBlueprintAndThenValidate(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(BlueprintEntity.class)
                .withName(blueprintName)
                .withAmbariBlueprint(VALID_BP)
                .when(Blueprint.postV4(), key(blueprintName))
                .select(bp -> bp.getResponse().getId(), key(blueprintName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(blueprintName)
                .withResourceType("blueprints")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForClusterTemplateAndThenValidate(TestContext testContext) {
        String clusterTemplateName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given("stackTemplate", StackTemplateEntity.class)
                .withEnvironment(EnvironmentEntity.class)
                .given(ClusterTemplateEntity.class)
                .withStackTemplate("stackTemplate")
                .withName(clusterTemplateName)
                .when(new ClusterTemplateV4CreateAction(), key(clusterTemplateName))
                .select(ct -> ct.getResponse().getId(), key(clusterTemplateName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(clusterTemplateName)
                .withResourceType("cluster_templates")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForCredentialAndThenValidate(TestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestAction::create, key(credentialName))
                .select(c -> c.getResponse().getId(), key(credentialName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(credentialName)
                .withResourceType("credentials")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForDatabaseAndThenValidate(TestContext testContext) {
        String databaseName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(DatabaseEntity.class)
                .withName(databaseName)
                .when(DatabaseEntity.post(), key(databaseName))
                .select(db -> db.getResponse().getId(), key(databaseName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(databaseName)
                .withResourceType("databases")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForEnvironmentAndThenValidate(TestContext testContext) {
        String environmentName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(EnvironmentEntity.class)
                .withName(environmentName)
                .when(Environment::post, key(environmentName))
                .select(env -> env.getResponse().getId(), key(environmentName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(environmentName)
                .withResourceType("environments")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForKerberosAndThenValidate(TestContext testContext) {
        String kerberosName = getNameGenerator().getRandomNameForMock();
        KerberosV4Request request = new KerberosV4Request();
        request.setName("adKerberos");
        ActiveDirectoryKerberosDescriptor activeDirectory = new ActiveDirectoryKerberosDescriptor();
        activeDirectory.setTcpAllowed(true);
        activeDirectory.setPrincipal("admin/principal");
        activeDirectory.setPassword("kerberosPassword");
        activeDirectory.setUrl("someurl.com");
        activeDirectory.setAdminUrl("admin.url.com");
        activeDirectory.setRealm("realm");
        activeDirectory.setLdapUrl("otherurl.com");
        activeDirectory.setContainerDn("{}");
        request.setActiveDirectory(activeDirectory);
        testContext
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(kerberosName)
                .when(KerberosTestAction::post, key(kerberosName))
                .select(env -> env.getResponse().getId(), key(kerberosName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(kerberosName)
                .withResourceType("kerberos")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForLdapAndThenValidate(TestContext testContext) {
        String ldapName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(LdapConfigTestDto.class)
                .withName(ldapName)
                .when(ldapConfigTestClient.post(), key(ldapName))
                .select(env -> env.getResponse().getId(), key(ldapName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(ldapName)
                .withResourceType("ldaps")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForMpacksAndThenValidate(TestContext testContext) {
        String mpackName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(MPackTestDto.class)
                .withName(mpackName)
                .when(MpackTestAction::create, key(mpackName))
                .select(env -> env.getResponse().getId(), key(mpackName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(mpackName)
                .withResourceType("mpacks")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForProxiesAndThenValidate(TestContext testContext) {
        String proxyName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(ProxyConfigEntity.class)
                .withName(proxyName)
                .when(ProxyConfig.postV4(), key(proxyName))
                .select(p -> p.getResponse().getId(), key(proxyName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(proxyName)
                .withResourceType("proxies")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForRecipesAndThenValidate(TestContext testContext) {
        String recipeName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(RecipeEntity.class)
                .withName(recipeName)
                .when(Recipe.postV4(), key(recipeName))
                .select(r -> r.getResponse().getId(), key(recipeName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(recipeName)
                .withResourceType("recipes")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateAuditForStacksAndThenValidate(TestContext testContext) {
        String stackName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackTestDto.class)
                .withEnvironment(EnvironmentEntity.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .select(env -> env.getResponse().getId(), key(stackName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(stackName)
                .withResourceType("stacks")
                .when(AuditTestAction::getAuditEvents)
                .then(AuditTestAssertion.listContainsAtLeast(1))
                .validate();
    }
}

