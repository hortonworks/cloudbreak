package com.sequenceiq.it.cloudbreak.testcase;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
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
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCloudStorageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.mock.ImageCatalogMockServerSetup;
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

    protected void createDatalake(TestContext testContext) {
        initiateDatalakeCreation(testContext);
        waitForDatalakeCreation(testContext);
    }

    protected void initiateDatalakeCreation(TestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                    .withCloudStorage(getCloudStorageRequest(testContext))
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

    protected void createDefaultDatahub(TestContext testContext) {
        createEnvironmentWithFreeIpaAndDatalake(testContext);
        initiateDefaultDatahubCreation(testContext);
        waitForDefaultDatahubCreation(testContext);
    }

    protected void initiateDefaultDatahubCreation(TestContext testContext) {
        testContext
                .given(DistroXTestDto.class)
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
    }

    protected void waitForUserSync(TestContext testContext) {
        testContext.given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
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
                    .withTunnel(Tunnel.CLUSTER_PROXY)
                    .withCreateFreeIpa(Boolean.TRUE)
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

    protected void createEnvironmentWithFreeIpaAndDatalake(TestContext testContext) {
        initiateEnvironmentCreation(testContext);
        initiateDatalakeCreation(testContext);
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
