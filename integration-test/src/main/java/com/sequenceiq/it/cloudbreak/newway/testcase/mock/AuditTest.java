package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.it.cloudbreak.newway.assertion.audit.AuditTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.AuditTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ClusterTemplateTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.KubernetesTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.MpackTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.mpack.MPackTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTemplateTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class AuditTest extends AbstractIntegrationTest {

    private static final String VALID_BP = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDP\",\"stack_version\":\"2.6\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\""
            + ",\"configurations\":[],\"components\":[{\"name\":\"HIVE_METASTORE\"}],\"cardinality\":\"1"
            + "\"}]}";

    private static final String IMG_CATALOG_URL = "https://cloudbreak-imagecatalog.s3.amazonaws.com/v2-prod-cb-image-catalog.json";

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private ClusterTemplateTestClient clusterTemplateTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private KubernetesTestClient kubernetesTestClient;

    @Inject
    private MpackTestClient mpackTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private AuditTestClient auditTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a recipe is created",
            then = "an and audit record must be available in the database")
    public void createValidRecipeThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String recipeName = resourcePropertyProvider().getName();
        testContext
                .given(RecipeTestDto.class)
                .withName(recipeName)
                .when(recipeTestClient.createV4(), key(recipeName))
                .select(recipe -> recipe.getResponse().getId(), key(recipeName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(recipeName)
                .withResourceType("recipes")
                .when(auditTestClient.listV4(), key(recipeName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(recipeName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a K8S config is created",
            then = "an audit record must be available in the database")
    public void createValidKubernetesConfigThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String kubernetesName = resourcePropertyProvider().getName();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(kubernetesTestClient.createV4(), key(kubernetesName))
                .select(kubernetes -> kubernetes.getResponse().getId(), key(kubernetesName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(kubernetesName)
                .withResourceType("kubernetes")
                .when(auditTestClient.listV4(), key(kubernetesName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(kubernetesName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Blueprint is created",
            then = "an audit record must be available in the database")
    public void createValidBlueprintThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(VALID_BP)
                .when(blueprintTestClient.createV4(), key(blueprintName))
                .select(bp -> bp.getResponse().getId(), key(blueprintName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(blueprintName)
                .withResourceType("blueprints")
                .when(auditTestClient.listV4(), key(blueprintName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(blueprintName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Cluster Template is created",
            then = "and audit record must be available in the database")
    public void createValidClusterTemplateThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String clusterTemplateName = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4(), key(clusterTemplateName))
                .given("stackTemplate", StackTemplateTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
                .withStackTemplate("stackTemplate")
                .withName(clusterTemplateName)
                .when(clusterTemplateTestClient.createV4(), key(clusterTemplateName))
                .select(ct -> ct.getResponse().getId(), key(clusterTemplateName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(clusterTemplateName)
                .withResourceType("cluster_templates")
                .when(auditTestClient.listV4(), key(clusterTemplateName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(clusterTemplateName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Credential is created",
            then = "and audit record must be available in the database")
    public void createValidCredentialThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String credentialName = resourcePropertyProvider().getName();
        testContext
                .given(CredentialTestDto.class)
                .withName(credentialName)
                .when(credentialTestClient.createV4(), key(credentialName))
                .select(c -> c.getResponse().getId(), key(credentialName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(credentialName)
                .withResourceType("credentials")
                .when(auditTestClient.listV4(), key(credentialName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(credentialName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Database is created",
            then = "and audit record must be available in the database")
    public void createValidDatabaseThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String databaseName = resourcePropertyProvider().getName();
        testContext
                .given(DatabaseTestDto.class)
                .withName(databaseName)
                .when(databaseTestClient.createV4(), key(databaseName))
                .select(db -> db.getResponse().getId(), key(databaseName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(databaseName)
                .withResourceType("databases")
                .when(auditTestClient.listV4(), key(databaseName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(databaseName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Environment is created",
            then = "and audit record must be available in the database")
    public void createValidEnvironmentThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String environmentName = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .withName(environmentName)
                .when(environmentTestClient.createV4(), key(environmentName))
                .select(env -> env.getResponse().getId(), key(environmentName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(environmentName)
                .withResourceType("environments")
                .when(auditTestClient.listV4(), key(environmentName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(environmentName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Kerberos is created",
            then = "and audit record must be available in the database")
    public void createValidKerberosThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String kerberosName = resourcePropertyProvider().getName();
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
                .when(kerberosTestClient.createV4(), key(kerberosName))
                .select(env -> env.getResponse().getId(), key(kerberosName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(kerberosName)
                .withResourceType("kerberos")
                .when(auditTestClient.listV4(), key(kerberosName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(kerberosName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Ldap is created",
            then = "and audit record must be available in the database")
    public void createValidLdapThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String ldapName = resourcePropertyProvider().getName();
        testContext
                .given(LdapTestDto.class)
                .withName(ldapName)
                .when(ldapTestClient.createV4(), key(ldapName))
                .select(env -> env.getResponse().getId(), key(ldapName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(ldapName)
                .withResourceType("ldaps")
                .when(auditTestClient.listV4(), key(ldapName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(ldapName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Mpack is created",
            then = "and audit record must be available in the database")
    public void createValidMpackThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String mpackName = resourcePropertyProvider().getName();
        testContext
                .given(MPackTestDto.class)
                .withName(mpackName)
                .when(mpackTestClient.createV4(), key(mpackName))
                .select(env -> env.getResponse().getId(), key(mpackName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(mpackName)
                .withResourceType("mpacks")
                .when(auditTestClient.listV4(), key(mpackName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(mpackName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Proxy is created",
            then = "and audit record must be available in the database")
    public void createValidProxyThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String proxyName = resourcePropertyProvider().getName();
        testContext
                .given(ProxyTestDto.class)
                .withName(proxyName)
                .when(proxyTestClient.createV4(), key(proxyName))
                .select(p -> p.getResponse().getId(), key(proxyName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(proxyName)
                .withResourceType("proxies")
                .when(auditTestClient.listV4(), key(proxyName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(proxyName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Stack is created",
            then = "and audit record must be available in the database")
    public void createValidStackThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String stackName = resourcePropertyProvider().getName();
        String auditName = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .select(env -> env.getResponse().getId(), key(stackName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(stackName)
                .withResourceType("stacks")
                .when(auditTestClient.listV4(), key(auditName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(auditName))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Image Catalog is created",
            then = "and audit record must be available in the database")
    public void createValidImageCatalogThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String catalogName = resourcePropertyProvider().getName();
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(catalogName)
                .withUrl(IMG_CATALOG_URL)
                .when(imageCatalogTestClient.createV4(), key(catalogName))
                .select(r -> r.getResponse().getId(), key(catalogName))
                .given(AuditTestDto.class)
                .withResourceIdByKey(catalogName)
                .withResourceType("image_catalogs")
                .when(auditTestClient.listV4(), key(catalogName))
                .then(AuditTestAssertion.listContainsAtLeast(1), key(catalogName))
                .validate();
    }
}

