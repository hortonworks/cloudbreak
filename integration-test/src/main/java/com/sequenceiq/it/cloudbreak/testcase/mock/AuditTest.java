package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.audit.AuditTestAssertion;
import com.sequenceiq.it.cloudbreak.client.AuditTestClient;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.ClusterTemplateTestClient;
import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.audit.AuditTestDto;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTemplateTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

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
    private RecipeTestClient recipeTestClient;

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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a Cluster Template is created",
            then = "and audit record must be available in the database")
    public void createValidClusterTemplateThenAuditRecordMustBeAvailableForTheResource(TestContext testContext) {
        String clusterTemplateName = resourcePropertyProvider().getName();
        testContext
                .given(EnvironmentTestDto.class)
                .given("stackTemplate", StackTemplateTestDto.class)
                .withEnvironment(EnvironmentTestDto.class)
                .given(ClusterTemplateTestDto.class)
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

}

