package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock.SALT_RUN;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

import spark.Route;

public class KerberosConfigTest extends AbstractIntegrationTest {

    private static final String LDAP_SYNC_PATH = "/api/v1/ldap_sync_events";

    private static final String SALT_HIGHSTATE = "state.highstate";

    private static final String BLUEPRINT_TEXT = "{\"Blueprints\":{\"blueprint_name\":\"ownbp\",\"stack_name\":\"HDF\",\"stack_version\":\"3.2\"},"
            + "\"settings\":[{\"recovery_settings\":[]},{\"service_settings\":[]},{\"component_settings\":[]}],\"configurations\":[],\"host_groups\":[{\"name\""
            + ":\"master\",\"configurations\":[],\"components\":[{\"name\":\"METRICS_MONITOR\"},{\"name\":\"METRICS_COLLECTOR\"},{\"name\":"
            + "\"ZOOKEEPER_CLIENT\"}],\"cardinality\":\"1\"}]}";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = "dataProviderForTest")
    public void testClusterCreationWithValidKerberos(MockedTestContext testContext, String blueprintName, KerberosTestData testData,
            @Description TestCaseDescription testCaseDescription) {
        mockAmbariBlueprintPassLdapSync(testContext);
        KerberosV4Request request = testData.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4())
                .given("master", InstanceGroupTestDto.class)
                .withHostGroup(MASTER)
                .withNodeCount(1)
                .given(ClusterTestDto.class)
                .withKerberos(request.getName())
                .withBlueprintName(blueprintName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .given(StackTestDto.class)
                .withInstanceGroups("master")
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(testData.getAssertions())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a cluster setup without kerberosname",
            when = "calling cluster creation",
            then = "the cluster should not been kerberized")
    public void testClusterCreationAttemptWithKerberosConfigWithoutName(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = resourcePropertyProvider().getName();
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .given("master", InstanceGroupTestDto.class)
                .withHostGroup(MASTER)
                .withNodeCount(1)
                .given(ClusterTestDto.class)
                .withKerberos(null)
                .withBlueprintName(blueprintName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .given(StackTestDto.class)
                .withInstanceGroups("master")
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a cluster setup with '' kerberosname",
            when = "calling cluster creation",
            then = "getting BadRequestException because kerberosname should not be empty")
    public void testClusterCreationAttemptWithKerberosConfigWithEmptyName(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = resourcePropertyProvider().getName();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4())
                .given("master", InstanceGroupTestDto.class)
                .withHostGroup(MASTER)
                .withNodeCount(1)
                .given(ClusterTestDto.class)
                .withKerberos("")
                .withBlueprintName(blueprintName)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .given(StackTestDto.class)
                .withInstanceGroups("master")
                .when(stackTestClient.createV4(), key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "an invalid kerberos descriptor JSON",
            when = "calling kerberos creation",
            then = "getting BadRequestException because descriptor should be a valid JSON")
    public void testKerberosCreationAttemptWhenDescriptorIsAnInvalidJson(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = resourcePropertyProvider().getName();
        String badRequest = resourcePropertyProvider().getName();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        String descriptor = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\",\"admin_server_host\""
                + ":\"admin-server-host-value\",\"realm\":\"realm-value\"}}";
        request.getAmbariDescriptor().setDescriptor(Base64.encodeBase64String(descriptor.getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4(), key(badRequest))
                .expect(BadRequestException.class, key(badRequest))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid kerberos descriptor JSON which does not contain all the required fields",
            when = "calling kerberos creation",
            then = "getting BadRequestException because descriptor need all the required fields")
    public void testKerberosCreationAttemptWhenDescriptorDoesNotContainsAllTheRequiredFields(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = resourcePropertyProvider().getName();
        String badRequest = resourcePropertyProvider().getName();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setDescriptor(
                Base64.encodeBase64String("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\"}}}".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4(), key(badRequest))
                .expect(BadRequestException.class, key(badRequest))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a kerberos configuration where the krb5conf is not a valid JSON",
            when = "calling kerberos creation",
            then = "getting BadRequestException because krb5conf should be a valid JSON")
    public void testKerberosCreationAttemptWhenKrb5ConfIsNotAValidJson(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = resourcePropertyProvider().getName();
        String badRequest = resourcePropertyProvider().getName();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setKrb5Conf(Base64.encodeBase64String("{".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(BlueprintTestDto.class)
                .withName(blueprintName)
                .withBlueprint(BLUEPRINT_TEXT)
                .when(blueprintTestClient.createV4())
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4(), key(badRequest))
                .expect(BadRequestException.class, key(badRequest))
                .validate();
    }

    @DataProvider(name = "dataProviderForTest")
    public Object[][] provide() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.FREEIPA,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a FreeIPA based kerberos configuration")
                                .when("calling calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                },
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.ACTIVE_DIRECTORY,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a Active Directory based kerberos configuration")
                                .when("calling calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                },
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.MIT,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a MIT based kerberos configuration")
                                .when("calling calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                },
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        KerberosTestData.AMBARI_DESCRIPTOR,
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a Ambari Descriptor based kerberos configuration")
                                .when("calling calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                }
        };
    }

    private void mockAmbariBlueprintPassLdapSync(MockedTestContext testContext) {
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
        return String.format("%s-%s", name, resourcePropertyProvider().getName());
    }

    private enum KerberosTestData {

        ACTIVE_DIRECTORY {
            @Override
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
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
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
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
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
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
            public List<AssertionV2<StackTestDto>> getAssertions() {
                List<AssertionV2<StackTestDto>> verifications = new LinkedList<>();
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

        public abstract List<AssertionV2<StackTestDto>> getAssertions();

        public abstract KerberosV4Request getRequest();

        private static MockVerification blueprintPostToAmbariContains(String content) {
            return MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").bodyContains(content);
        }

    }

}