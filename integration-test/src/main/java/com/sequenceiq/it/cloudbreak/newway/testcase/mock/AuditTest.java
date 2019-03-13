package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

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
import com.sequenceiq.it.cloudbreak.newway.action.imagecatalog.ImageCatalogPostAction;
import com.sequenceiq.it.cloudbreak.newway.action.kerberos.KerberosTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.kubernetes.KubernetesTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.mpack.MpackTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.recipe.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.assertion.audit.AuditTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.ClusterDefinitionTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapConfigTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.database.DatabaseEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfig;
import com.sequenceiq.it.cloudbreak.newway.entity.proxy.ProxyConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class AuditTest extends AbstractIntegrationTest {

    private static final String VALID_BP = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"}],\"cardinality\":\"1"
            + "\"}]}";

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

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
        initializeDefaultClusterDefinitions(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((MockedTestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a recipe is created",
            then = "an and audit record must be available in the database")
    public void createValidRecipeThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String recipeName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .when(RecipeTestClient::postV4, key(recipeName))
                .select(recipe -> recipe.getResponse().getId(), key(recipeName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(recipeName)
                .withResourceType("recipes")
                .when(AuditTestAction::getAuditEvents, key(recipeName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(recipeName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a K8S config is created",
            then = "an audit record must be available in the database")
    public void createValidKubernetesConfigThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String kubernetesName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(KubernetesTestAction::create, key(kubernetesName))
                .select(kubernetes -> kubernetes.getResponse().getId(), key(kubernetesName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(kubernetesName)
                .withResourceType("kubernetes")
                .when(AuditTestAction::getAuditEvents, key(kubernetesName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(kubernetesName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Cluster Definition is created",
            then = "an audit record must be available in the database")
    public void createValidClusterDefinitionThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String blueprintName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(ClusterDefinitionTestDto.class)
                .withName(blueprintName)
                .withClusterDefinition(VALID_BP)
                .when(ClusterDefinitionTestClient.postV4(), key(blueprintName))
                .select(bp -> bp.getResponse().getId(), key(blueprintName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(blueprintName)
                .withResourceType("cluster_definitions")
                .when(AuditTestAction::getAuditEvents, key(blueprintName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Cluster Template is created",
            then = "and audit record must be available in the database")
    public void createValidClusterTemplateThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String clusterTemplateName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post, key(clusterTemplateName))
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
                .when(AuditTestAction::getAuditEvents, key(clusterTemplateName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(clusterTemplateName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Credential is created",
            then = "and audit record must be available in the database")
    public void createValidCredentialThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String credentialName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(CredentialTestClient::create, key(credentialName))
                .select(c -> c.getResponse().getId(), key(credentialName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(credentialName)
                .withResourceType("credentials")
                .when(AuditTestAction::getAuditEvents, key(credentialName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(credentialName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Database is created",
            then = "and audit record must be available in the database")
    public void createValidDatabaseThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String databaseName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(DatabaseEntity.class)
                .withName(databaseName)
                .when(DatabaseEntity.post(), key(databaseName))
                .select(db -> db.getResponse().getId(), key(databaseName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(databaseName)
                .withResourceType("databases")
                .when(AuditTestAction::getAuditEvents, key(databaseName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(databaseName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Environment is created",
            then = "and audit record must be available in the database")
    public void createValidEnvironmentThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String environmentName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(EnvironmentEntity.class)
                .withName(environmentName)
                .when(Environment::post, key(environmentName))
                .select(env -> env.getResponse().getId(), key(environmentName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(environmentName)
                .withResourceType("environments")
                .when(AuditTestAction::getAuditEvents, key(environmentName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(environmentName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Kerberos is created",
            then = "and audit record must be available in the database")
    public void createValidKerberosThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String kerberosName = getNameGenerator().getRandomNameForResource();
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
                .when(AuditTestAction::getAuditEvents, key(kerberosName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(kerberosName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Ldap is created",
            then = "and audit record must be available in the database")
    public void createValidLdapThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String ldapName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(LdapConfigTestDto.class)
                .withName(ldapName)
                .when(ldapConfigTestClient.post(), key(ldapName))
                .select(env -> env.getResponse().getId(), key(ldapName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(ldapName)
                .withResourceType("ldaps")
                .when(AuditTestAction::getAuditEvents, key(ldapName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(ldapName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Mpack is created",
            then = "and audit record must be available in the database")
    public void createValidMpackThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String mpackName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(MPackTestDto.class)
                .withName(mpackName)
                .when(MpackTestAction::create, key(mpackName))
                .select(env -> env.getResponse().getId(), key(mpackName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(mpackName)
                .withResourceType("mpacks")
                .when(AuditTestAction::getAuditEvents, key(mpackName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(mpackName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Proxy is created",
            then = "and audit record must be available in the database")
    public void createValidProxyThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String proxyName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(ProxyConfigEntity.class)
                .withName(proxyName)
                .when(ProxyConfig.postV4(), key(proxyName))
                .select(p -> p.getResponse().getId(), key(proxyName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(proxyName)
                .withResourceType("proxies")
                .when(AuditTestAction::getAuditEvents, key(proxyName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(proxyName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Stack is created",
            then = "and audit record must be available in the database")
    public void createValidStackThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String stackName = getNameGenerator().getRandomNameForResource();
        String auditName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(EnvironmentEntity.class)
                .when(Environment::post)
                .given(StackTestDto.class)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .select(env -> env.getResponse().getId(), key(stackName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(stackName)
                .withResourceType("stacks")
                .when(AuditTestAction::getAuditEvents, key(auditName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(auditName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Image Catalog is created",
            then = "and audit record must be available in the database")
    public void createValidImageCatalogThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String catalogName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(catalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(new ImageCatalogPostAction(), key(catalogName))
                .select(r -> r.getResponse().getId(), key(catalogName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(catalogName)
                .withResourceType("image_catalogs")
                .when(AuditTestAction::getAuditEvents, key(catalogName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(catalogName))
                .validate();
    }
}

