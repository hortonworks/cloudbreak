package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.newway.mock.model.SaltMock.SALT_RUN;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.it.cloudbreak.newway.Blueprint;
import com.sequenceiq.it.cloudbreak.newway.BlueprintEntity;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
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

    @Inject
    private RandomNameCreator creator;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = "dataProviderForTest")
    public void testClusterCreationWithValidKerberos(TestContext testContext, String blueprintName, KerberosTestData testData) {
        mockAmbariBlueprintPassLdapSync(testContext);
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withAmbariRequest(new AmbariEntity(testContext)
                                .valid()
                                .withKerberos(testData.getRequest())
                                .withBlueprintName(blueprintName)
                                .withEnableSecurity(true)))
                .when(Stack.postV2())
                .await(STACK_AVAILABLE)
                .then(testData.getAssertions())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testClusterCreationAttemptWithKerberosWhenDescriptorIsAnInvalidJson(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = creator.getRandomNameForMock();
        KerberosRequest request = KerberosTestData.CUSTOM.getRequest();
        request.setDescriptor("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\",\"admin_server_host\""
                + ":\"admin-server-host-value\",\"realm\":\"realm-value\"}}");
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withAmbariRequest(new AmbariEntity(testContext)
                                .valid()
                                .withKerberos(request)
                                .withBlueprintName(blueprintName)
                                .withEnableSecurity(true)))
                .when(Stack.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testClusterCreationAttemptWithKerberosWhenDescriptorDoesNotContainsAllTheRequiredFields(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = creator.getRandomNameForMock();
        KerberosRequest request = KerberosTestData.CUSTOM.getRequest();
        request.setDescriptor("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\"}}}");
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withAmbariRequest(new AmbariEntity(testContext)
                                .valid()
                                .withKerberos(request)
                                .withBlueprintName(blueprintName)
                                .withEnableSecurity(true)))
                .when(Stack.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    public void testClusterCreationAttemptWithKerberosWhenKrb5ConfIsNotAValidJson(TestContext testContext) {
        mockAmbariBlueprintPassLdapSync(testContext);
        String blueprintName = creator.getRandomNameForMock();
        KerberosRequest request = KerberosTestData.CUSTOM.getRequest();
        request.setKrb5Conf("{");
        testContext
                .given(BlueprintEntity.class).valid().withName(blueprintName).withAmbariBlueprint(BLUEPRINT_TEXT)
                .when(Blueprint.postV2())
                .given("master", InstanceGroupEntity.class).valid().withHostGroup(MASTER).withNodeCount(1)
                .given(StackEntity.class)
                .withInstanceGroups("master")
                .withCluster(new ClusterEntity(testContext)
                        .valid()
                        .withAmbariRequest(new AmbariEntity(testContext)
                                .valid()
                                .withKerberos(request)
                                .withBlueprintName(blueprintName)
                                .withEnableSecurity(true)))
                .when(Stack.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @DataProvider(name = "dataProviderForTest")
    public Object[][] provide() {
        return new Object[][]{
                {applicationContext.getBean(TestContext.class), creator.getRandomNameForMock(), KerberosTestData.CB_MANAGED},
                {applicationContext.getBean(TestContext.class), creator.getRandomNameForMock(), KerberosTestData.EXISTING_AD},
                {applicationContext.getBean(TestContext.class), creator.getRandomNameForMock(), KerberosTestData.EXISTING_MIT},
                {applicationContext.getBean(TestContext.class), creator.getRandomNameForMock(), KerberosTestData.CUSTOM}
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

    private enum KerberosTestData {

        CB_MANAGED {
            @Override
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains(getRequest().getMasterKey()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getPassword()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getAdmin()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
                verifications.addAll(verifyCloudbreakUserHasSentToCreate());
                return verifications;
            }

            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setTcpAllowed(false);
                request.setMasterKey("masterKey");
                request.setAdmin("kerberosAdmin");
                request.setPassword("kerberosPassw0rd");
                request.setType(KerberosType.CB_MANAGED);
                return request;
            }
        },

        EXISTING_AD {
            @Override
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains(getRequest().getPassword()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getPrincipal()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getAdminUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getLdapUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getContainerDn()).exactTimes(1));
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
                verifications.addAll(verifyCloudbreakUserHasSentToCreate());
                return verifications;
            }

            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setTcpAllowed(true);
                request.setPrincipal("admin/principal");
                request.setPassword("kerberosPassword");
                request.setUrl("someurl.com");
                request.setAdminUrl("admin.url.com");
                request.setRealm("realm");
                request.setLdapUrl("otherurl.com");
                request.setContainerDn("{}");
                request.setType(KerberosType.EXISTING_AD);
                return request;
            }
        },

        EXISTING_MIT {
            @Override
            public List<AssertionV2<StackEntity>> getAssertions() {
                List<AssertionV2<StackEntity>> verifications = new LinkedList<>();
                verifications.add(blueprintPostToAmbariContains(getRequest().getRealm().toUpperCase()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getPassword()).exactTimes(0));
                verifications.add(blueprintPostToAmbariContains(getRequest().getAdminUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains(getRequest().getUrl()).exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("mit-kdc").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("realm").exactTimes(1));
                verifications.add(blueprintPostToAmbariContains("KERBEROS_CLIENT").exactTimes(1));
                verifications.add(MockVerification.verify(HttpMethod.POST, SALT_RUN).bodyContains(SALT_HIGHSTATE).exactTimes(2));
                verifications.addAll(verifyCloudbreakUserHasSentToCreate());
                return verifications;
            }

            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setTcpAllowed(true);
                request.setPrincipal("kerberosPrincipal");
                request.setPassword("kerberosPassword");
                request.setUrl("kerberosproviderurl.com");
                request.setAdminUrl("kerberosadminurl.com");
                request.setRealm("kerbRealm");
                request.setType(KerberosType.EXISTING_MIT);
                return request;
            }
        },

        CUSTOM {
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
                verifications.addAll(verifyCloudbreakUserHasSentToCreate());
                return verifications;
            }

            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setTcpAllowed(true);
                request.setPrincipal("kerberosPrincipal");
                request.setPassword("kerberosPassword");
                request.setDescriptor("{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"mit-kdc\",\"kdc_hosts\":\"kdc-host-value\",\"admin_server_host\":"
                        + "\"admin-server-host-value\",\"realm\":\"realm-value\"}}}");
                request.setKrb5Conf("{}");
                request.setType(KerberosType.CUSTOM);
                return request;
            }
        };

        public abstract List<AssertionV2<StackEntity>> getAssertions();

        public abstract KerberosRequest getRequest();

        private static MockVerification blueprintPostToAmbariContains(String content) {
            return MockVerification.verify(HttpMethod.POST, "/api/v1/blueprints/").bodyContains(content);
        }

        private static List<AssertionV2<StackEntity>> verifyCloudbreakUserHasSentToCreate() {
            return List.of(
                    MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").bodyContains("\"Users/active\": true"),
                    MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").bodyContains("\"Users/admin\": true"),
                    MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").bodyContains("\"Users/user_name\": \"cloudbreak\""),
                    MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").bodyContains("Users/password"));
        }

    }

}