package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiConfigList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.mock.model.SaltMock;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

import spark.Route;

public class KerberosConfigTest extends AbstractIntegrationTest {

    private static final String LDAP_SYNC_PATH = "/api/v1/ldap_sync_events";

    private static final String SALT_HIGHSTATE = "state.highstate";

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = "dataProviderForTest", enabled = false)
    public void testClusterCreationWithValidKerberos(MockedTestContext testContext, String blueprintName, KerberosTestData testData,
            @Description TestCaseDescription testCaseDescription) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.put(ClouderaManagerMock.API_ROOT + "/cm/config", (request, response) -> new ApiConfigList());
        dynamicRouteStack
                .post(ClouderaManagerMock.API_ROOT
                        + "/cm/commands/importAdminCredentials", (request, response) -> new ApiCommand().id(new BigDecimal(1)));

        KerberosV4Request request = testData.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4())
                .given("master", InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .withNodeCount(1)
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(testContext))
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(testData.getAssertions())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a cluster setup without kerberosname",
            when = "calling cluster creation",
            then = "the cluster should not been kerberized")
    public void testClusterCreationAttemptWithKerberosConfigWithoutName(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        testContext
                .given("master", InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .withNodeCount(1)
                .given(ClusterTestDto.class)
                .given(StackTestDto.class)
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(testContext))
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a cluster setup with '' kerberosname",
            when = "calling cluster creation",
            then = "getting BadRequestException because kerberosname should not be empty")
    public void testClusterCreationAttemptWithKerberosConfigWithEmptyName(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4())
                .given("master", InstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .withNodeCount(1)
                .given(ClusterTestDto.class)
                .withAmbari(testContext.given(AmbariTestDto.class))
                .given(StackTestDto.class)
                .withInstanceGroupsEntity(InstanceGroupTestDto.defaultHostGroup(testContext))
                .when(stackTestClient.createV4(), key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a valid kerberos descriptor JSON which does not contain all the required fields",
            when = "calling kerberos creation",
            then = "getting BadRequestException because descriptor need all the required fields")
    public void testKerberosCreationAttemptWhenDescriptorDoesNotContainsAllTheRequiredFields(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String badRequest = resourcePropertyProvider().getName();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setDescriptor(
                Base64.encodeBase64String("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\"}}}".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
                .given(KerberosTestDto.class)
                .withRequest(request)
                .withName(request.getName())
                .when(kerberosTestClient.createV4(), key(badRequest))
                .expect(BadRequestException.class, key(badRequest))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a kerberos configuration where the krb5conf is not a valid JSON",
            when = "calling kerberos creation",
            then = "getting BadRequestException because krb5conf should be a valid JSON")
    public void testKerberosCreationAttemptWhenKrb5ConfIsNotAValidJson(MockedTestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String badRequest = resourcePropertyProvider().getName();
        KerberosV4Request request = KerberosTestData.AMBARI_DESCRIPTOR.getRequest();
        request.getAmbariDescriptor().setKrb5Conf(Base64.encodeBase64String("{".getBytes()));
        request.setName(extendNameWithGeneratedPart(request.getName()));
        testContext
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
            public List<Assertion<StackTestDto, CloudbreakClient>> getAssertions() {
                List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
                verifications.add(clusterTemplatePostToCMContains("enableKerberos").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
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
            public List<Assertion<StackTestDto, CloudbreakClient>> getAssertions() {
                List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
                verifications.add(clusterTemplatePostToCMContains("enableKerberos").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
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
            public List<Assertion<StackTestDto, CloudbreakClient>> getAssertions() {
                List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
                verifications.add(clusterTemplatePostToCMContains("enableKerberos").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SaltMock.SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
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
            public List<Assertion<StackTestDto, CloudbreakClient>> getAssertions() {
                List<Assertion<StackTestDto, CloudbreakClient>> verifications = new LinkedList<>();
                verifications.add(clusterTemplatePostToCMContains("enableKerberos").exactTimes(1));
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

        public abstract List<Assertion<StackTestDto, CloudbreakClient>> getAssertions();

        public abstract KerberosV4Request getRequest();

        private static MockVerification clusterTemplatePostToCMContains(String content) {
            return MockVerification.verify(HttpMethod.POST, ClouderaManagerMock.IMPORT_CLUSTERTEMPLATE).bodyContains(content);
        }

    }

}