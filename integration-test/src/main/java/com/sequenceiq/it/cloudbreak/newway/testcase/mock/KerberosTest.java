package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock.SALT_RUN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.action.clusterdefinition.ClusterDefinitionTestAction;
import com.sequenceiq.it.cloudbreak.newway.action.kerberos.KerberosTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

import spark.Route;

public class KerberosTest extends AbstractIntegrationTest {

    private static final String LDAP_SYNC_PATH = "/api/v1/ldap_sync_events";

    private static final String SALT_HIGHSTATE = "state.highstate";

    private static final String CLUSTER_DEFINITION_TEXT = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDF\",\"stack_version\":\"3.2\"},"
            + "\"settings\":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\""
            + ":\"master\",\"configurations\":[],\"components\":[{\"name\":\"METRICS_MONITOR\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":"
            + "\"ZOOKEEPER_CLIENT\"}],\"cardinality\":\"1\"}]}";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((MockedTestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "dataProviderForTest")
    public void testClusterCreationWithValidKerberos(MockedTestContext testContext, String clusterDefinitionName, KerberosTestData testData) {
        mockAmbariClusterDefinitionPassLdapSync(testContext);
        KerberosV4Request request = testData.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .given(KerberosTestDto.class).withRequest(request).withName(request.getName())
                .when(KerberosTestAction::post)
                .given("master", InstanceGroupEntity.class).withHostGroup(MASTER).withNodeCount(1)
                .given(StackTestDto.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .withKerberos(request.getName())
                        .withClusterDefinitionName(clusterDefinitionName)
                        .withAmbari(testContext.given(AmbariEntity.class)))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .then(testData.getAssertions())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testClusterCreationAttemptWithKerberosConfigWithoutName(MockedTestContext testContext) {
        mockAmbariClusterDefinitionPassLdapSync(testContext);
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .given("master", InstanceGroupEntity.class).withHostGroup(MASTER).withNodeCount(1)
                .given(StackTestDto.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .withKerberos(null).withClusterDefinitionName(clusterDefinitionName)
                        .withAmbari(testContext.given(AmbariEntity.class)))
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testClusterCreationAttemptWithKerberosConfigWithEmptyName(MockedTestContext testContext) {
        mockAmbariClusterDefinitionPassLdapSync(testContext);
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .given(KerberosTestDto.class).withRequest(request).withName(request.getName())
                .when(KerberosTestAction::post)
                .given("master", InstanceGroupEntity.class).withHostGroup(MASTER).withNodeCount(1)
                .given(StackTestDto.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .withKerberos("")
                        .withClusterDefinitionName(clusterDefinitionName)
                        .withAmbari(testContext.given(AmbariEntity.class)))
                .when(Stack.postV4(), key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testKerberosCreationAttemptWhenDescriptorIsAnInvalidJson(MockedTestContext testContext) {
        mockAmbariClusterDefinitionPassLdapSync(testContext);
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        String descriptor = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\",\"admin_server_host\""
                + ":\"admin-server-host-value\",\"realm\":\"realm-value\"}}";
        request.getAmbariDescriptor().setDescriptor(Base64.encodeBase64String(descriptor.getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .given(KerberosTestDto.class).withRequest(request).withName(request.getName())
                .when(KerberosTestAction::post, key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testKerberosCreationAttemptWhenDescriptorDoesNotContainsAllTheRequiredFields(MockedTestContext testContext) {
        mockAmbariClusterDefinitionPassLdapSync(testContext);
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setDescriptor(
                Base64.encodeBase64String("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\"}}}".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .given(KerberosTestDto.class).withRequest(request).withName(request.getName())
                .when(KerberosTestAction::post, key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testKerberosCreationAttemptWhenKrb5ConfIsNotAValidJson(MockedTestContext testContext) {
        mockAmbariClusterDefinitionPassLdapSync(testContext);
        String clusterDefinitionName = getNameGenerator().getRandomNameForResource();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setKrb5Conf(Base64.encodeBase64String("{".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(ClusterDefinitionTestDto.class).withName(clusterDefinitionName).withClusterDefinition(CLUSTER_DEFINITION_TEXT)
                .when(ClusterDefinitionTestAction.postV4())
                .given(KerberosTestDto.class).withRequest(request).withName(request.getName())
                .when(KerberosTestAction::post, key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @DataProvider(name = "dataProviderForTest")
    public Object[][] provide() {
        return new Object[][]{
                {getBean(MockedTestContext.class), getNameGenerator().getRandomNameForResource(), KerberosTestData.FREEIPA},
                {getBean(MockedTestContext.class), getNameGenerator().getRandomNameForResource(), KerberosTestData.ACTIVE_DIRECTORY},
                {getBean(MockedTestContext.class), getNameGenerator().getRandomNameForResource(), KerberosTestData.MIT},
                {getBean(MockedTestContext.class), getNameGenerator().getRandomNameForResource(), KerberosTestData.AMBARI_DESCRIPTOR}
        };
    }

    private void mockAmbariClusterDefinitionPassLdapSync(MockedTestContext testContext) {
        Route customResponse2 = (request, response) -> {
            if (LDAP_SYNC_PATH.equals(request.url())) {
                response.type(TEXT_PLAIN);
                response.status(OK.getStatusCode());
            }
            return "";
        };
        testContext.getModel().getAmbariMock().getDynamicRouteStack().clearPost(LDAP_SYNC_PATH);
        testContext.getModel().getAmbariMock().getDynamicRouteStack().post(LDAP_SYNC_PATH, customResponse2);
    }

    private String extendNameWithGeneratedPart(String name) {
        return String.format("%s-%s", name, getNameGenerator().getRandomNameForResource());
    }

    private enum KerberosTestData {

        ACTIVE_DIRECTORY {
            @Override
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getActiveDirectory().getPassword()).exactTimes(0));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getActiveDirectory().getPrincipal()).exactTimes(0));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getActiveDirectory().getUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getActiveDirectory().getAdminUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getActiveDirectory().getLdapUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getActiveDirectory().getContainerDn()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("active-directory").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("realm").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("kdc_type").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("kdc_hosts").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("admin_server_host").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("encryption_types").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("ldap_url").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("container_dn").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
                return verifications;
            }

            @Override
            public KerberosV4Request getRequest() {
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
                return request;
            }
        },

        MIT {
            @Override
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getMit().getRealm().toUpperCase()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getMit().getPassword()).exactTimes(0));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getMit().getAdminUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getMit().getUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("mit-kdc").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("realm").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
                return verifications;
            }

            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                request.setName("mitKerberos");
                MITKerberosDescriptor mit = new MITKerberosDescriptor();
                mit.setTcpAllowed(true);
                mit.setPrincipal("kerberosPrincipal");
                mit.setPassword("kerberosPassword");
                mit.setUrl("kerberosproviderurl.com");
                mit.setAdminUrl("kerberosadminurl.com");
                mit.setRealm("kerbRealm");
                request.setMit(mit);
                return request;
            }
        },

        AMBARI_DESCRIPTOR {
            @Override
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
                verifications.add(clusterDefinitionPostToAmbariContains("kdc_type").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("mit-kdc").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("kdc_hosts").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("kdc-host-value").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("admin_server_host").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("admin-server-host-value").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("realm").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("realm-value").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
                return verifications;
            }

            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                request.setName("customKerberos");
                AmbariKerberosDescriptor ambariKerberosDescriptor = new AmbariKerberosDescriptor();
                ambariKerberosDescriptor.setTcpAllowed(true);
                ambariKerberosDescriptor.setPrincipal("kerberosPrincipal");
                ambariKerberosDescriptor.setPassword("kerberosPassword");
                String descriptor = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\",\"admin_server_host\""
                        + ":\"admin-server-host-value\",\"realm\":\"realm-value\"}}}";
                ambariKerberosDescriptor.setDescriptor(Base64.encodeBase64String(descriptor.getBytes()));
                ambariKerberosDescriptor.setKrb5Conf(Base64.encodeBase64String("{}".getBytes()));
                request.setAmbariDescriptor(ambariKerberosDescriptor);
                return request;
            }
        },

        FREEIPA {
            @Override
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
                verifications.add(clusterDefinitionPostToAmbariContains("kdc_type").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("ipa").exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getFreeIpa().getUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains(getRequest().getFreeIpa().getAdminUrl()).exactTimes(1));
                verifications.add(clusterDefinitionPostToAmbariContains("\"case_insensitive_username_rules\":\"true\"").exactTimes(1));
                return verifications;
            }

            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                FreeIPAKerberosDescriptor freeIpaRequest = new FreeIPAKerberosDescriptor();
                freeIpaRequest.setAdminUrl("http://someurl.com");
                freeIpaRequest.setRealm("someRealm");
                freeIpaRequest.setUrl("http://someadminurl.com");
                freeIpaRequest.setRealm("realm");
                freeIpaRequest.setPassword("freeipapassword");
                freeIpaRequest.setPrincipal("kerberosPrincipal");
                request.setName("freeIpaTest");
                request.setDescription("some free ipa description");
                request.setFreeIpa(freeIpaRequest);
                return request;
            }
        };

        public abstract List<AssertionV2<StackTestDto>> getAssertions();

        public abstract KerberosV4Request getRequest();

        private static MockVerification clusterDefinitionPostToAmbariContains(String content) {
            return MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").bodyContains(content);
        }

    }

}