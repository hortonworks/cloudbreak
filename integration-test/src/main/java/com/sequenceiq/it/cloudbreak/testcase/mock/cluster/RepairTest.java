package com.sequenceiq.it.cloudbreak.testcase.mock.cluster;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.freeipa.api.v1.kerberos.model.create.CreateKerberosConfigRequest;
import com.sequenceiq.it.cloudbreak.action.ClusterRepairAction;
import com.sequenceiq.it.cloudbreak.action.v4.database.DatabaseCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.ambari.AmbariClusterRequestResponse;
import com.sequenceiq.it.cloudbreak.mock.ambari.AmbariGetHostComponentsReponse;
import com.sequenceiq.it.cloudbreak.mock.ambari.AmbariGetServiceComponentInfoResponse;
import com.sequenceiq.it.cloudbreak.mock.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class RepairTest extends AbstractIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepairTest.class);

    private static final int HTTP_CREATED = 201;

    private static final String AMBARI_HOST_COMPONENTS = "/api/v1/clusters/:cluster/hosts/:host/host_components";

    private static final String AMBARI_CLUSTER_COMPONENTS = "api/v1/clusters/:cluster/components/:component";

    private static final String AMBARI_KERBEROS_CREDENTIAL = "/api/v1/clusters/:cluster/credentials/kdc.admin.credential";

    private static final String AMBARI_REGENERATE_KEYTABS = "/api/v1/clusters/:cluster?regenerate_keytabs=all";

    private static final String AMBARI_CLUSTER_REQUESTS = "/api/v1/clusters/:cluster/requests";

    private final Set<String> components = Set.of("component-new-liga", "component-liga-client", "component-yellow-submarine");

    @Inject
    private KerberosTestClient kerberosTestAction;

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContext();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a MOCK cluster without kerberos",
            when = "repair the master node on the cluster",
            then = "after the process the master has to be available")
    public void testRepairMasterNodeNoKerberos(MockedTestContext testContext) {
        String ambariRdsName = resourcePropertyProvider().getName();
        String clusterName = resourcePropertyProvider().getName();
        String hostnameKey = resourcePropertyProvider().getName();
        createEnvWithResources(testContext);
        addAmbariMocks(testContext, clusterName);
        testContext
                .given(StackTestDto.class)
                .withName(clusterName)
                .withCluster(getCluster(testContext, null, ambariRdsName))
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceMetaData(HostGroupType.MASTER.getName())
                        .stream()
                        .findFirst()
                        .get()
                        .getDiscoveryFQDN(), key(hostnameKey));

        testContext
                .given(StackTestDto.class)
                .when(ClusterRepairAction.valid())
                .await(StackTestDto.class, STACK_AVAILABLE);

        testContext.given(StackTestDto.class)
                .then((tc, stackEntity, cc) -> {
                    String hostname = tc.getSelected(hostnameKey);
                    AmbariPathResolver ambariPathResolver = new AmbariPathResolver(clusterName, hostname);
                    assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "INSTALLED", 2, ambariPathResolver);
                    assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "INIT", 1, ambariPathResolver);
                    assertSetComponentStateCalls(stackEntity, AMBARI_HOST_COMPONENTS, components, "STARTED", 1, ambariPathResolver);
                    stackEntity.then(MockVerification.verify(HttpMethod.GET,
                            ambariPathResolver.resolve(AMBARI_HOST_COMPONENTS)).exactTimes(2));
                    stackEntity.then(MockVerification.verify(HttpMethod.PUT,
                            ambariPathResolver.resolve(AMBARI_HOST_COMPONENTS)).exactTimes(4));
                    stackEntity.then(MockVerification.verify(HttpMethod.POST,
                            ambariPathResolver.resolve(AMBARI_KERBEROS_CREDENTIAL)).exactTimes(0));
                    stackEntity.then(MockVerification.verify(HttpMethod.PUT,
                            ambariPathResolver.resolve(AMBARI_REGENERATE_KEYTABS)).bodyContains("KERBEROS")
                            .exactTimes(0));
                    stackEntity.then(MockVerification.verify(HttpMethod.POST,
                            ambariPathResolver.resolve(AMBARI_CLUSTER_REQUESTS)).bodyContains("RESTART")
                            .exactTimes(0));
                    return stackEntity;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK, enabled = false)
    @Description(
            given = "a MOCK cluster with kerberos",
            when = "repair the master node on the cluster",
            then = "after the process the master has to be available")
    public void testRepairMasterNodeWithKerberos(MockedTestContext testContext) {
        String ambariRdsName = resourcePropertyProvider().getName();
        createEnvWithResources(testContext);
        CreateKerberosConfigRequest kerberosRequest = getKerberosRequest();
        String clusterName = resourcePropertyProvider().getName();
        addAmbariMocks(testContext, clusterName);
        testContext
                .given(KerberosTestDto.class).withRequest(kerberosRequest).withName(kerberosRequest.getName())
                .when(kerberosTestAction.createV1())
                .given(StackTestDto.class)
                .withName(clusterName)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(getCluster(testContext, kerberosRequest.getName(), ambariRdsName))
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .select(s -> s.getInstanceMetaData(HostGroupType.MASTER.getName()).stream().findFirst().get().getDiscoveryFQDN(), key("hostname"));

        testContext
                .given(StackTestDto.class)
                .when(ClusterRepairAction.valid())
                .await(StackTestDto.class, STACK_AVAILABLE);

        testContext.given(StackTestDto.class)
                .then((tc, e, cc) -> {
                    String hostname = tc.getSelected("hostname");
                    AmbariPathResolver ambariPathResolver = new AmbariPathResolver(clusterName, hostname);
                    assertSetComponentStateCalls(e, AMBARI_HOST_COMPONENTS, components, "INSTALLED", 2, ambariPathResolver);
                    assertSetComponentStateCalls(e, AMBARI_HOST_COMPONENTS, components, "INIT", 1, ambariPathResolver);
                    assertSetComponentStateCalls(e, AMBARI_HOST_COMPONENTS, components, "STARTED", 1, ambariPathResolver);
                    e.then(MockVerification.verify(HttpMethod.GET, ambariPathResolver.resolve(AMBARI_HOST_COMPONENTS)).exactTimes(2));
                    e.then(MockVerification.verify(HttpMethod.POST, ambariPathResolver.resolve(AMBARI_KERBEROS_CREDENTIAL)));
                    e.then(MockVerification.verify(HttpMethod.PUT, ambariPathResolver.resolve(AMBARI_REGENERATE_KEYTABS)).bodyContains("KERBEROS"));
                    e.then(MockVerification.verify(HttpMethod.POST, ambariPathResolver.resolve(AMBARI_CLUSTER_REQUESTS)).bodyContains("RESTART"));
                    return e;
                })
                .validate();
    }

    private void assertGetClusterComponentCalls(StackTestDto stackEntity, String path, Set<String> components, AmbariPathResolver ambariPathResolver) {
        components.forEach(component -> stackEntity.then(MockVerification.verify(HttpMethod.GET, ambariPathResolver.resolveComponent(path, component))));
    }

    private void assertSetComponentStateCalls(StackTestDto stackEntity, String path, Set<String> components, String expectedState, int times,
            AmbariPathResolver ambariPathResolver) {
        MockVerification mockVerification = MockVerification.verify(HttpMethod.PUT, ambariPathResolver.resolve(path))
                .bodyContains(expectedState);
        components.forEach(mockVerification::bodyContains);
        mockVerification.atLeast(times);
        stackEntity.then(mockVerification);
    }

    private void createEnvWithResources(MockedTestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.create());
    }

    private ClusterTestDto getCluster(MockedTestContext testContext, String kerberosConfigName, String ambariRdsName) {
        testContext.given(ClusterTestDto.class)
                .valid()
                .withRdsConfigNames(ambariRdsName);

//        if (kerberosConfigName != null && !kerberosConfigName.isEmpty()) {
//            testContext.given(ClusterTestDto.class).withKerberos(kerberosConfigName);
//        }

        return testContext.given(ClusterTestDto.class);
    }

    private Set<String> createAmbariRdsConfig(MockedTestContext testContext, String ambariRdsName) {

        testContext
                .given(DatabaseTestDto.class)
                .withType(DatabaseType.AMBARI.name())
                .withName(ambariRdsName)
                .when(new DatabaseCreateIfNotExistsAction());
        Set<String> validRds = new HashSet<>();
        validRds.add(getRdsConfigName(testContext));
        return validRds;
    }

    private String getRdsConfigName(MockedTestContext testContext) {
        return testContext.get(DatabaseTestDto.class).getName();
    }

    private String getEnvironmentName(MockedTestContext testContext) {
        return testContext.get(EnvironmentTestDto.class).getName();
    }

    private String getEnvironmentRegion(MockedTestContext testContext) {
        return testContext.get(EnvironmentTestDto.class).getRequest().getRegions().iterator().next();
    }

    private void addAmbariMocks(MockedTestContext testContext, String clusterName) {
        DynamicRouteStack dynamicRouteStack = testContext.getModel().getAmbariMock().getDynamicRouteStack();
        dynamicRouteStack.get(AMBARI_HOST_COMPONENTS, new AmbariGetHostComponentsReponse(components, clusterName));
        components.forEach(comp -> dynamicRouteStack.get(AMBARI_CLUSTER_COMPONENTS, new AmbariGetServiceComponentInfoResponse(comp)));
        dynamicRouteStack.put(AMBARI_HOST_COMPONENTS, new AmbariClusterRequestResponse(testContext.getModel().getMockServerAddress(), clusterName));
        dynamicRouteStack.post(AMBARI_KERBEROS_CREDENTIAL, new EmptyAmbariResponse(HTTP_CREATED));
        dynamicRouteStack.put(AMBARI_REGENERATE_KEYTABS, new AmbariClusterRequestResponse(testContext.getModel().getMockServerAddress(), clusterName));
        dynamicRouteStack.post(AMBARI_CLUSTER_REQUESTS, new AmbariClusterRequestResponse(testContext.getModel().getMockServerAddress(), clusterName));
    }

    private CreateKerberosConfigRequest getKerberosRequest() {
        CreateKerberosConfigRequest request = new CreateKerberosConfigRequest();
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

    private static class AmbariPathResolver {
        private static final String VAR_CLUSTER = ":cluster";

        private static final String VAR_HOSTNAME = ":host";

        private static final String VAR_COMPONENT = ":component";

        private final String hostName;

        private final String clusterName;

        private AmbariPathResolver(String clusterName, String hostName) {
            this.hostName = hostName;
            this.clusterName = clusterName;
        }

        private String resolve(String path) {
            return resolvePath(path);
        }

        private String resolveComponent(String path, String component) {
            return resolvePath(path)
                    .replaceAll(VAR_COMPONENT, component);
        }

        private String resolvePath(String path) {
            return eraseQuery(path)
                    .replaceAll(VAR_CLUSTER, clusterName)
                    .replaceAll(VAR_HOSTNAME, hostName);
        }

        private String eraseQuery(String pathWithQuery) {
            return pathWithQuery.split("\\?")[0];
        }
    }
}
