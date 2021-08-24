package com.sequenceiq.it.cloudbreak.testcase;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

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
    private RedbeamsDatabaseTestClient databaseTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @BeforeMethod
    public final void minimalSetupForClusterCreation(Object[] data, ITestResult testResult) {
        setupTest(testResult);
        setupTest((TestContext) data[0]);
    }

    protected void setupTest(ITestResult testResult) {

    }

    protected void setupTest(TestContext testContext) {

    }

    public EnvironmentTestClient getEnvironmentTestClient() {
        return environmentTestClient;
    }

    protected void createImageValidationSourceCatalog(TestContext testContext, String url, String name) {
        testContext.given(ImageCatalogTestDto.class)
                .withUrl(url)
                .withName(name)
                .withoutCleanup()
                .when(imageCatalogTestClient.createIfNotExistV4());
    }

    protected void validatePrewarmedImage(TestContext testContext, String imageUuid) {
        testContext.given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4(true))
                .valid();
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        dto.getResponse().getImages().getCdhImages().stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(imageUuid + " prewarmed image is missing from the '" + dto.getName() + "' catalog."));
    }

    protected void validateBaseImage(TestContext testContext, String imageUuid) {
        testContext.given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.getV4(true))
                .valid();
        ImageCatalogTestDto dto = testContext.get(ImageCatalogTestDto.class);
        dto.getResponse().getImages().getBaseImages().stream()
                .filter(img -> img.getUuid().equalsIgnoreCase(imageUuid))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(imageUuid + " base image is missing from the '" + dto.getName() + "' catalog."));
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

    protected void createDatalake(TestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    protected void createDatalakeWithoutDatabase(TestContext testContext) {
        SdxDatabaseRequest database = new SdxDatabaseRequest();
        database.setCreate(false);

        testContext
                .given(SdxInternalTestDto.class)
                    .withDatabase(database)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    protected void createDatahub(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.get())
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
                .given(RedbeamsDatabaseTestDto.class)
                .when(databaseTestClient.createIfNotExistV4());
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(RedbeamsDatabaseTestDto.class).getName());
        return validRds;
    }

    protected void createEnvironmentWithNetworkAndFreeIpa(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withTunnel(Tunnel.CLUSTER_PROXY)
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                    .withCatalog(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                    .withTelemetry("telemetry")
                    .withNetwork()
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .validate();
    }

    protected void createEnvironmentWithNetwork(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createImageCatalogWithUrl(TestContext testContext, String name, String url) {
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .withUrl(url)
                .when(new ImageCatalogCreateRetryAction())
                .validate();
    }

    protected void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    protected void createSecondUser(TestContext testContext) {
        testContext.as(cloudbreakActor.secondUser());
    }

    protected void useRealUmsUser(TestContext testContext, String key) {
        testContext
                .as(cloudbreakActor.useRealUmsUser(key))
                .useUmsUserCache(true);
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

    protected SdxCloudStorageRequest getCloudStorageRequest(TestContext testContext) {
        String storage = resourcePropertyProvider().getName();
        testContext.given(storage, SdxCloudStorageTestDto.class);
        SdxCloudStorageTestDto cloudStorage = testContext.getCloudProvider().cloudStorage(testContext.get(storage));
        if (cloudStorage == null) {
            throw new IllegalArgumentException("SDX Cloud Storage does not exist!");
        }
        return cloudStorage.getRequest();
    }
}
