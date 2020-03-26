package com.sequenceiq.it.cloudbreak.testcase;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;

public abstract class AbstractIntegrationTest extends AbstractMinimalTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @BeforeMethod
    public final void minimalSetupForClusterCreation(Object[] data) {
        setupTest((TestContext) data[0]);
    }

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultFreeIPA(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    public EnvironmentTestClient getEnvironmentTestClient() {
        return environmentTestClient;
    }

    protected void createDefaultFreeIPA(TestContext testContext) {
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;
        setUpFreeIpaRouteStubbing(mockedTestContext);
        testContext
                .given(FreeIPATestDto.class).withCatalog(mockedTestContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE)
                .validate();
    }

    protected void setUpFreeIpaRouteStubbing(MockedTestContext mockedTestContext) {
        DynamicRouteStack dynamicRouteStack = mockedTestContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/login_password", (request, response) -> {
            response.cookie("ipa_session", "dummysession");
            return "";
        });
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/user_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/user_mod", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/role_add_member", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/cert_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/host_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/service_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/dnszone_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/dnszone_add", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/dnszone_del", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/dnsrecord_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/host_del", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/role_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/server_conncheck", freeIpaRouteHandler);
    }

    protected void createImageValidationSourceCatalog(TestContext testContext, String url, String name) {
        testContext.given(ImageCatalogTestDto.class)
                .withUrl(url)
                .withName(name)
                .withoutCleanup()
                .when(imageCatalogTestClient.createIfNotExistV4());
    }

    protected void createDefaultEnvironment(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createDefaultEnvironmentWithNetwork(TestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createDefaultCredential(TestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .validate();
    }

    protected void createDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogCreateRetryAction())
                .validate();
    }

    protected Set<String> createDefaultLdapConfig(TestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .when(ldapTestClient.createIfNotExistV1());
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        return validLdap;
    }

    protected Set<String> createDefaultKerberosConfig(TestContext testContext) {
        testContext
                .given(ActiveDirectoryKerberosDescriptorTestDto.class)
                .withDomain(null)
                .withRealm("realm.addomain.com")
                .given(KerberosTestDto.class)
                .withActiveDirectoryDescriptor()
                .when(kerberosTestClient.createV1());
        Set<String> validKerberos = new HashSet<>();
        validKerberos.add(testContext.get(KerberosTestDto.class).getName());
        return validKerberos;
    }

    protected Set<String> createDefaultRdsConfig(TestContext testContext) {
        testContext
                .given(DatabaseTestDto.class)
                .when(databaseTestClient.createIfNotExistV4());
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        return validRds;
    }

    protected void createEnvironmentWithNetworkAndFreeIPA(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createEnvironmentWithoutTelemetry(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createEnvironmentForSdx(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .withS3Guard()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createImageCatalog(TestContext testContext, String name) {
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .when(imageCatalogTestClient.createV4(), key(name))
                .validate();
    }

    protected void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    protected void createSecondUser(TestContext testContext) {
        testContext.as(Actor::secondUser);
    }

    protected void initializeDefaultBlueprints(TestContext testContext) {
        testContext
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4())
                .validate();
    }

    protected void initializeAzureCloudStorage(TestContext testContext) {
        azureCloudBlobUtil.createContainerIfNotExist();
    }

    protected EnvironmentNetworkRequest environmentNetwork() {
        EnvironmentNetworkRequest networkReq = new EnvironmentNetworkRequest();
        networkReq.setNetworkCidr("0.0.0.0/0");
        EnvironmentNetworkMockParams mockReq = new EnvironmentNetworkMockParams();
        mockReq.setVpcId("vepeceajdi");
        mockReq.setInternetGatewayId("1.1.1.1");
        networkReq.setMock(mockReq);
        networkReq.setSubnetIds(Set.of("net1", "net2"));
        return networkReq;
    }

    public FreeIpaRouteHandler getFreeIpaRouteHandler() {
        return freeIpaRouteHandler;
    }
}
