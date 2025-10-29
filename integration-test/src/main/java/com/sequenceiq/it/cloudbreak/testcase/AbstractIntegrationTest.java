package com.sequenceiq.it.cloudbreak.testcase;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.util.Strings;

import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
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
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
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
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
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
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private AzureMarketplaceTermsClient azureMarketplaceTermsClient;

    @Inject
    private EnvironmentUtil environmentUtil;

    @BeforeMethod
    public final void minimalSetupForClusterCreation(Object[] data, ITestResult testResult) {
        BouncyCastleFipsProviderLoader.load();
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

        testContext
                .given(SdxInternalTestDto.class)
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
        waitForDatahubCreation(testContext);
    }

    protected void createDefaultDatahubWithAutoTlsAndExternalDbForExistingDatalake(TestContext testContext) {
        initiateDefaultDatahubCreationWithAutoTlsAndExternalDb(testContext);
        waitForDatahubCreation(testContext);
    }

    protected void createDataMartDatahubWithAutoTlsAndExternalDbForExistingDatalake(TestContext testContext) {
        initiateDataMartDatahubCreationWithAutoTlsAndExternalDb(testContext);
        waitForDatahubCreation(testContext);
    }

    protected void createDefaultDatahub(TestContext testContext) {
        createDefaultDatalake(testContext);
        createDefaultDatahubForExistingDatalake(testContext);
    }

    protected void createDefaultDatahubWithAutoTlsAndExternalDb(TestContext testContext) {
        createDefaultDatalakeWithAutoTlsAndExternalDb(testContext);
        createDefaultDatahubWithAutoTlsAndExternalDbForExistingDatalake(testContext);
    }

    protected void createDataMartDatahubWithAutoTlsAndExternalDb(TestContext testContext) {
        createDefaultDatalakeWithAutoTlsAndExternalDb(testContext);
        createDataMartDatahubWithAutoTlsAndExternalDbForExistingDatalake(testContext);
    }

    protected void createStorageOptimizedDatahub(TestContext testContext) {
        createDefaultDatalake(testContext);
        initiateStorageOptimizedDatahubCreation(testContext);
        waitForDatahubCreation(testContext);
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

    protected void initiateDataMartDatahubCreationWithAutoTlsAndExternalDb(TestContext testContext) {
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA);

        testContext
                .given(DistroXTestDto.class)
                .withAutoTls()
                .withExternalDatabase(databaseRequest)
                .withCluster(testContext.given(DistroXClusterTestDto.class)
                        .withBlueprintName(testContext.getCloudProvider().getDataMartDistroXBlueprintName()))
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataMartHostGroups(testContext, testContext.getCloudPlatform()))
                .when(distroXTestClient.create())
                .validate();
    }

    protected void waitForDatahubCreation(TestContext testContext) {
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

    protected void createDefaultCredential(TestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .validate();
    }

    protected void createExtendedCredential(TestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .withExtendedArn()
                .when(credentialTestClient.create())
                .validate();
    }

    protected void createDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.createIfNotExistV4())
                .validate();
        createImageValidationImageCatalog(testContext);
    }

    protected void createImageValidationImageCatalog(TestContext testContext) {
        String catalogName = commonCloudProperties().getImageValidation().getSourceCatalogName();
        String catalogUrl = commonCloudProperties().getImageValidation().getSourceCatalogUrl();
        if (Strings.isNotNullAndNotEmpty(catalogName) && Strings.isNotNullAndNotEmpty(catalogUrl)) {
            testContext
                    .given(ImageCatalogTestDto.class)
                    .withName(catalogName)
                    .withUrl(catalogUrl)
                    .withoutCleanup()
                    .when(imageCatalogTestClient.createIfNotExistV4())
                    .validate();
        }
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

    protected void createEnvironmentWithFreeIpa(TestContext testContext, Architecture architecture) {
        initiateEnvironmentCreation(testContext, architecture);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
    }

    protected void createEnvironmentWithFreeIpa(TestContext testContext) {
        createEnvironmentWithFreeIpa(testContext, null);
    }

    protected void waitForUserSync(TestContext testContext) {
        testContext.given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .validate();
    }

    protected void setWorkloadPassword(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .validate();
        waitForUserSync(testContext);
        if (!testContext.isMockUms()) {
            testContext
                    .given(UmsTestDto.class).assignTarget(EnvironmentTestDto.class.getSimpleName())
                    .when(umsTestClient.setWorkloadPassword(testContext.getWorkloadPassword()))
                    .given(FreeIpaUserSyncTestDto.class)
                    .when(freeIpaTestClient.syncAll())
                    .await(OperationState.COMPLETED)
                    .given(FreeIpaTestDto.class)
                    .when(freeIpaTestClient.describe())
                    .validate();
        }
    }

    protected void setFreeIpaResponse(TestContext testContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .validate();
    }

    protected void initiateEnvironmentCreation(TestContext testContext, Architecture architecture) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withTunnel(testContext.getTunnel())
                .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                .withCreateFreeIpa(Boolean.TRUE)
                .withFreeIpaArchitecture(architecture)
                .withFreeIpaNodes(getFreeIpaInstanceCountByProvider(testContext))
                .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .validate();
    }

    protected void initiateEnvironmentCreation(TestContext testContext) {
        initiateEnvironmentCreation(testContext, null);
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
        setFreeIpaResponse(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void createDatalakeWithVersion(TestContext testContext, String runtimeVersion) {
        initiateEnvironmentCreation(testContext);
        testContext
                .given(SdxInternalTestDto.class)
                .withRuntimeVersion(runtimeVersion);
        initiateDatalakeCreation(testContext);
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void createDefaultDatalakeWithAutoTlsAndExternalDb(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        initiateDatalakeCreationWithAutoTlsAndExternalDb(testContext);
        waitForEnvironmentCreation(testContext);
        setFreeIpaResponse(testContext);
        waitForUserSync(testContext);
        setFreeIpaResponse(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    protected void useRealUmsUser(TestContext testContext, String key) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(key);
    }

    protected void initializeDefaultBlueprints(TestContext testContext) {
        testContext
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4())
                .validate();
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

    protected int getFreeIpaInstanceCountByProvider(TestContext testContext) {
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
                .given(SdxTestDto.class)
                .withName(datalakeName)
                .when(sdxTestClient.describe());
    }

    /**
     * Helper method to speed up local testing. Could be invoked to re-use an already existing datalake with the given name
     */
    protected void useExistingInternalDatalake(TestContext testContext, String datalakeName) {
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
