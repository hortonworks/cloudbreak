package com.sequenceiq.it.cloudbreak.testcase;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.AzureMarketplaceTermsClient;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.util.EnvironmentUtil;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
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
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private AzureMarketplaceTermsClient azureMarketplaceTermsClient;

    @Inject
    private EnvironmentUtil environmentUtil;

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

    protected void initializeAzureMarketplaceTermsPolicy(TestContext testContext) {
        testContext.init(TermsPolicyDto.class)
                .withAccepted(Boolean.TRUE)
                .when(azureMarketplaceTermsClient.put())
                .validate();
    }

    protected void createDatalake(TestContext testContext) {
        initiateDatalakeCreation(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void initiateDatalakeCreation(TestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withEnableMultiAz()
                    .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .validate();
    }

    protected void initiateDatalakeCreationWithAutoTlsAndExternalDb(TestContext testContext) {
        SdxDatabaseRequest databaseRequest = new SdxDatabaseRequest();
        databaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);

        testContext
                .given(SdxInternalTestDto.class)
                    .withDatabase(databaseRequest)
                    .withAutoTls()
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withEnableMultiAz()
                    .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .validate();
    }

    protected void waitForDatalakeCreation(TestContext testContext) {
        testContext.given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    protected void createDatalakeWithoutDatabase(TestContext testContext) {
        SdxDatabaseRequest database = new SdxDatabaseRequest();
        database.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        database.setCreate(false);

        testContext
                .given(SdxInternalTestDto.class)
                    .withDatabase(database)
                    .withCloudStorage(getCloudStorageRequest(testContext))
                    .withEnableMultiAz()
                    .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    protected void createDefaultDatahubForExistingDatalake(TestContext testContext) {
        initiateDefaultDatahubCreation(testContext);
        waitForDefaultDatahubCreation(testContext);
    }

    protected void createDefaultDatahubWithAutoTlsAndExternalDbForExistingDatalake(TestContext testContext) {
        initiateDefaultDatahubCreationWithAutoTlsAndExternalDb(testContext);
        waitForDefaultDatahubCreation(testContext);
    }

    protected void createDefaultDatahub(TestContext testContext) {
        createDefaultDatalake(testContext);
        createDefaultDatahubForExistingDatalake(testContext);
    }

    protected void createDefaultDatahubWithAutoTlsAndExternalDb(TestContext testContext) {
        createDefaultDatalakeWithAutoTlsAndExternalDb(testContext);
        createDefaultDatahubWithAutoTlsAndExternalDbForExistingDatalake(testContext);
    }

    protected void createStorageOptimizedDatahub(TestContext testContext) {
        createDefaultDatalake(testContext);
        initiateStorageOptimizedDatahubCreation(testContext);
        waitForDefaultDatahubCreation(testContext);
    }

    protected void initiateDefaultDatahubCreation(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .validate();
    }

    protected void initiateDefaultDatahubCreationWithAutoTlsAndExternalDb(TestContext testContext) {
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given(DistroXTestDto.class)
                .withAutoTls()
                .withExternalDatabase(databaseRequest)
                .when(distroXTestClient.create())
                .validate();
    }

    protected void waitForDefaultDatahubCreation(TestContext testContext) {
        testContext.given(DistroXTestDto.class)
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .when(distroXTestClient.get())
                .validate();
    }

    protected void initiateStorageOptimizedDatahubCreation(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
                .withInstanceGroupsEntity(new DistroXInstanceGroupsBuilder(testContext)
                        .defaultHostGroup()
                        .withStorageOptimizedInstancetype()
                        .build())
                .when(distroXTestClient.create())
                .validate();
    }

    protected void createDatahubWithDatabase(TestContext testContext) {
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given(DistroXTestDto.class)
                    .withExternalDatabase(databaseRequest)
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

    protected void createEnvironmentWithFreeIpa(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
    }

    protected void waitForUserSync(TestContext testContext) {
        testContext.given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .validate();
    }

    private void setFreeIpaResponse(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .validate();
    }

    protected void initiateEnvironmentCreation(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withTunnel(testContext.getTunnel())
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withFreeIpaNodes(getFreeIpaInstanceCountByProdiver(testContext))
                    .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                            commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .validate();
    }

    protected void waitForEnvironmentCreation(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .validate();
    }

    protected void createDefaultDatalake(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        initiateDatalakeCreation(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void createDefaultDatalakeWithAutoTlsAndExternalDb(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        initiateDatalakeCreationWithAutoTlsAndExternalDb(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void createDefaultUser(TestContext testContext) {
        testContext.as();
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

    protected void createDatahubInEnvironment(TestContext testContext, String environmentName) {
        testContext
                .given(DistroXTestDto.class)
                .withEnvironmentName(environmentName)
                .when(distroXTestClient.create())
                .validate();
        waitForDefaultDatahubCreation(testContext);
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

    protected int getFreeIpaInstanceCountByProdiver(TestContext testContext) {
        return environmentUtil.getFreeIpaInstanceCountByProdiver(testContext);
    }

    protected void createProxyConfig(TestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .when(proxyTestClient.createIfNotExist())
                .validate();
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing environment with the given name
     */
    protected void useExistingEnvironment(TestContext testContext, String environmentName) {
        testContext
                .given(EnvironmentTestDto.class)
                    .withName(environmentName)
                .when(environmentTestClient.describe())
                .validate();
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing environment and FreeIpa with the given name
     */
    protected void useExistingEnvironmentWithFreeipa(TestContext testContext, String environmentName) {
        useExistingEnvironment(testContext, environmentName);
        setFreeIpaResponse(testContext);
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing datalake with the given name
     */
    protected void useExistingDatalake(TestContext testContext, String datalakeName) {
        testContext
                .given(SdxInternalTestDto.class)
                    .withName(datalakeName)
                .when(sdxTestClient.describeInternal());
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing datahub with the given name
     */
    protected void useExistingDatahub(TestContext testContext, String datahubName) {
        testContext
                .given(DistroXTestDto.class)
                    .withName(datahubName)
                .when(distroXTestClient.get())
                .validate();
    }
}
