package com.sequenceiq.it.cloudbreak.newway.testcase;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.Kerberos;
import com.sequenceiq.it.cloudbreak.newway.KerberosEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.AmbariEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceGroupEntity;

import spark.Route;

public class KerberosTest extends AbstractIntegrationTest {

    private static final String TEST_CONTEXT = "testContext";

    private static final String LDAP_SYNC_PATH = "/api/v1/ldap_sync_events";

    private static final String SALT_HIGHSTATE = "state.highstate";

    private static final String BLUEPRINT_TEXT = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDF\",\"stack_version\":\"3.2\"},\"settings\""
            + ":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\":\"master\","
            + "\"configurations\":[],\"components\":[{\"name\":\"METRICS_MONITOR\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":\"ZOOKEEPER_CLIENT\"}],"
            + "\"cardinality\":\"1\"}]}";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((TestContext) data[0]);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "dataProviderForTest")
    public void testClusterCreationWithValidKerberos(TestContext testContext, String blueprintName, KerberosTestData testData) {
        mockAmbariBlueprintPassLdapSync(testContext);
        KerberosV4Request request = testData.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given(KerberosEntity.class).valid().withRequest(request).withName(request.getName())
                .when(Kerberos.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withKerberos(request.getName())
                        .withAmbari(new AmbariEntity(testContext)
                                .valid()
                                .withBlueprintName(blueprintName)))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .then(testData.getAssertions())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testClusterCreationAttemptWithKerberosConfigWithoutName(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withKerberos(null)
                        .withAmbari(new AmbariEntity(testContext)
                                .valid()
                                .withBlueprintName(blueprintName)))
                .when(Stack.postV4())
                .await(Status.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testClusterCreationAttemptWithKerberosConfigWithEmptyName(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = getNameGenerator().getRandomNameForMock();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given(KerberosEntity.class).valid().withRequest(request).withName(request.getName())
                .when(Kerberos.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withKerberos("")
                        .withAmbari(new AmbariEntity(testContext)
                                .valid()
                                .withBlueprintName(blueprintName)))
                .when(Stack.postV4(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testKerberosCreationAttemptWhenDescriptorIsAnInvalidJson(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = getNameGenerator().getRandomNameForMock();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        String descriptor = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\",\"admin_server_host\""
                + ":\"admin-server-host-value\",\"realm\":\"realm-value\"}}";
        request.getAmbariDescriptor().setDescriptor(Base64.encodeBase64String(descriptor.getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given(KerberosEntity.class).valid().withRequest(request).withName(request.getName())
                .when(Kerberos.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testKerberosCreationAttemptWhenDescriptorDoesNotContainsAllTheRequiredFields(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = getNameGenerator().getRandomNameForMock();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setDescriptor(
                Base64.encodeBase64String("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\"}}}".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given(KerberosEntity.class).valid().withRequest(request).withName(request.getName())
                .when(Kerberos.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testKerberosCreationAttemptWhenKrb5ConfIsNotAValidJson(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = getNameGenerator().getRandomNameForMock();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setKrb5Conf(Base64.encodeBase64String("{".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given(KerberosEntity.class).valid().withRequest(request).withName(request.getName())
                .when(Kerberos.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @DataProvider(name = "dataProviderForTest")
    public Object[][] provide() {
        return new Object[][]{
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), KerberosTestData.FREEIPA},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), KerberosTestData.ACTIVE_DIRECTORY},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), KerberosTestData.MIT},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), KerberosTestData.AMBARI_DESCRIPTOR}
        };
    }

    private void mockAmbariBlueprintPassLdapSync(TestContext testContext) {
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
        return String.format("%s-%s", name, getNameGenerator().getRandomNameForMock());
    }

    private enum KerberosTestData {

        ACTIVE_DIRECTORY {
            @Override
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains(getRequest().getActiveDirectory().getPassword()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getActiveDirectory().getPrincipal()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getActiveDirectory().getUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getActiveDirectory().getAdminUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getActiveDirectory().getLdapUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getActiveDirectory().getContainerDn()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("active-directory").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("realm").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("kdc_type").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("kdc_hosts").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("admin_server_host").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("encryption_types").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("ldap_url").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("container_dn").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
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
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains(getRequest().getMit().getRealm().toUpperCase()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getMit().getPassword()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getMit().getAdminUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getMit().getUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("mit-kdc").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("realm").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
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
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains("kdc_type").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("mit-kdc").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("kdc_hosts").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("kdc-host-value").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("admin_server_host").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("admin-server-host-value").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("realm").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("realm-value").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
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
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains("kdc_type").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("ipa").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getFreeIpa().getUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getFreeIpa().getAdminUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("\"case_insensitive_username_rules\":\"true\"").exactTimes(1));
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

        public abstract List<AssertionV2<StackEntity>> getAssertions();

        public abstract KerberosV4Request getRequest();

        private static MockVerification blueprintPostToAmbariContains(String content) {
            return MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").bodyContains(content);
        }

    }

}