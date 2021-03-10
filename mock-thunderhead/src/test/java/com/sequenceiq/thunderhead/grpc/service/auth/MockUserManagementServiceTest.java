package com.sequenceiq.thunderhead.grpc.service.auth;

import static com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder.INTERNAL_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.migrationsupport.rules.ExpectedExceptionSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Entitlement;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ListTermsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadPasswordPolicy;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.thunderhead.util.JsonUtil;

import io.grpc.StatusRuntimeException;
import io.grpc.internal.testing.StreamRecorder;

@ExtendWith(ExpectedExceptionSupport.class)
@ExtendWith(MockitoExtension.class)
public class MockUserManagementServiceTest {

    private static final String VALID_LICENSE = "License file content";

    private static final String SAMPLE_SSH_PUBLIC_KEY = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGHA5cmj5+agIalPxw85jFrrZcSh3dl06ukeqKu6JVQm nobody@example.com";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Spy
    private MockCrnService mockCrnService;

    @InjectMocks
    private MockUserManagementService underTest;

    @Mock
    private JsonUtil jsonUtil;

    @Test
    public void testSetLicenseShouldReturnACloudbreakLicense() throws IOException {
        Path licenseFilePath = Files.createTempFile("license", "txt");
        Files.writeString(licenseFilePath, VALID_LICENSE);
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", licenseFilePath.toString());

        try {
            underTest.init();

            String actual = ReflectionTestUtils.getField(underTest, "cbLicense").toString();

            assertThat(actual).isEqualTo(VALID_LICENSE);
        } finally {
            Files.delete(licenseFilePath);
        }
    }

    @Test
    public void testSetLicenseShouldEmptyStringWhenTheFileIsNotExists() {
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", "/etc/license");

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("The license file could not be found on path: '/etc/license'. " +
                "Please place your CM license file in your '<cbd_path>/etc' folder. By default the name of the file should be 'license.txt'.");

        underTest.init();
    }

    @Test
    public void testCreateWorkloadUsername() {
        String username = "&*foO$_#Bar22@baz13.com";
        String expected = "foo_bar22";

        assertThat(underTest.sanitizeWorkloadUsername(username)).isEqualTo(expected);
    }

    @Test
    public void testGetWorkloadCredentials() throws IOException {
        Path sshPublicKeyFilePath = Files.createTempFile("key", ".pub");
        Files.writeString(sshPublicKeyFilePath, SAMPLE_SSH_PUBLIC_KEY);
        ReflectionTestUtils.setField(underTest, "sshPublicKeyFilePath", sshPublicKeyFilePath.toString());

        try {
            underTest.initializeActorWorkloadCredentials();

            long currentTime = System.currentTimeMillis();

            GetActorWorkloadCredentialsRequest req = GetActorWorkloadCredentialsRequest.newBuilder()
                    .setActorCrn(Crn.builder(CrnResourceDescriptor.USER)
                            .setAccountId(UUID.randomUUID().toString())
                            .setResource(UUID.randomUUID().toString())
                            .build().toString())
                    .build();
            StreamRecorder<GetActorWorkloadCredentialsResponse> observer = StreamRecorder.create();

            underTest.getActorWorkloadCredentials(req, observer);

            assertThat(observer.getValues().size()).isEqualTo(1);
            GetActorWorkloadCredentialsResponse res = observer.getValues().get(0);
            assertThat(res).isNotNull();
            assertThat(res.getPasswordHash()).isNotNull();
            assertThat(res.getKerberosKeysList()).isNotNull();
            assertThat(res.getKerberosKeysList().size()).isEqualTo(2);
            assertThat(res.getPasswordHashExpirationDate() > currentTime).isTrue();
            assertThat(res.getSshPublicKeyCount()).isEqualTo(1);
            assertThat(res.getSshPublicKey(0).getPublicKey()).isEqualTo(SAMPLE_SSH_PUBLIC_KEY);
        } finally {
            Files.delete(sshPublicKeyFilePath);
        }
    }

    @Test
    public void testGetAccountIncludesPasswordPolicy() throws IOException {
        Path licenseFilePath = Files.createTempFile("license", "txt");
        Files.writeString(licenseFilePath, VALID_LICENSE);
        ReflectionTestUtils.setField(underTest, "cmLicenseFilePath", licenseFilePath.toString());

        try {
            underTest.init();

            GetAccountRequest req = GetAccountRequest.getDefaultInstance();
            StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();

            underTest.getAccount(req, observer);

            assertThat(observer.getValues().size()).isEqualTo(1);
            GetAccountResponse res = observer.getValues().get(0);
            assertThat(res.hasAccount()).isTrue();
            Account account = res.getAccount();
            assertThat(account.hasGlobalPasswordPolicy()).isTrue();
            WorkloadPasswordPolicy passwordPolicy = account.getGlobalPasswordPolicy();
            assertThat(passwordPolicy.getWorkloadPasswordMaxLifetime()).isEqualTo(MockUserManagementService.PASSWORD_LIFETIME);
        } finally {
            Files.delete(licenseFilePath);
        }
    }

    @Test
    void getAccountTestIncludesFixedEntitlements() {
        ReflectionTestUtils.setField(underTest, "cbLicense", VALID_LICENSE);
        underTest.initializeWorkloadPasswordPolicy();

        GetAccountRequest req = GetAccountRequest.getDefaultInstance();
        StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();

        underTest.getAccount(req, observer);

        assertThat(observer.getValues().size()).isEqualTo(1);
        GetAccountResponse res = observer.getValues().get(0);
        assertThat(res.hasAccount()).isTrue();
        Account account = res.getAccount();
        List<String> entitlements = account.getEntitlementsList().stream().map(Entitlement::getEntitlementName).collect(Collectors.toList());
        assertThat(entitlements).contains("CDP_AZURE", "CDP_GCP", "CDP_AUTOMATIC_USERSYNC_POLLER", "CLOUDERA_INTERNAL_ACCOUNT", "DATAHUB_AZURE_AUTOSCALING",
                "DATAHUB_AWS_AUTOSCALING", "LOCAL_DEV", "DATAHUB_FLOW_SCALING", "DATAHUB_STREAMING_SCALING", "CDP_CM_ADMIN_CREDENTIALS");
    }

    static Object[][] conditionalEntitlementDataProvider() {
        return new Object[][]{
                // testCaseName conditionFieldName condition entitlementName entitlementPresentExpected
                {"enableBaseImages false", "enableBaseImages", false, "CDP_BASE_IMAGE", false},
                {"enableBaseImages true", "enableBaseImages", true, "CDP_BASE_IMAGE", true},

                {"enableFreeIpaHaRepair false", "enableFreeIpaHaRepair", false, "CDP_FREEIPA_HA_REPAIR", false},
                {"enableFreeIpaHaRepair true", "enableFreeIpaHaRepair", true, "CDP_FREEIPA_HA_REPAIR", true},

                {"enableCloudStorageValidation false", "enableCloudStorageValidation", false, "CDP_CLOUD_STORAGE_VALIDATION", false},
                {"enableCloudStorageValidation true", "enableCloudStorageValidation", true, "CDP_CLOUD_STORAGE_VALIDATION", true},

                {"runtimeUpgradeEnabled false", "runtimeUpgradeEnabled", false, "CDP_RUNTIME_UPGRADE", false},
                {"runtimeUpgradeEnabled true", "runtimeUpgradeEnabled", true, "CDP_RUNTIME_UPGRADE", true},

                {"datahubRuntimeUpgradeEnabled false", "datahubRuntimeUpgradeEnabled", false, "CDP_RUNTIME_UPGRADE_DATAHUB", false},
                {"datahubRuntimeUpgradeEnabled true", "datahubRuntimeUpgradeEnabled", true, "CDP_RUNTIME_UPGRADE_DATAHUB", true},

                {"razEnabled false", "razEnabled", false, "CDP_RAZ", false},
                {"razEnabled true", "razEnabled", true, "CDP_RAZ", true},

                {"rawS3Enabled false", "rawS3Enabled", false, "CDP_RAW_S3", false},
                {"rawS3Enabled true", "rawS3Enabled", true, "CDP_RAW_S3", true},

                {"ccmV2Enabled false", "ccmV2Enabled", false, "CDP_CCM_V2", false},
                {"ccmV2Enabled true", "ccmV2Enabled", true, "CDP_CCM_V2", true},

                {"mediumDutySdxEnabled false", "mediumDutySdxEnabled", false, "CDP_MEDIUM_DUTY_SDX", false},
                {"mediumDutySdxEnabled true", "mediumDutySdxEnabled", true, "CDP_MEDIUM_DUTY_SDX", true},

                {"enableAzureSingleResourceGroupDeployment false", "enableAzureSingleResourceGroupDeployment", false, "CDP_AZURE_SINGLE_RESOURCE_GROUP", false},
                {"enableAzureSingleResourceGroupDeployment true", "enableAzureSingleResourceGroupDeployment", true, "CDP_AZURE_SINGLE_RESOURCE_GROUP", true},

                {"enableAzureSingleResourceGroupDedicatedStorageAccount false", "enableAzureSingleResourceGroupDedicatedStorageAccount", false,
                        "CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT", false},
                {"enableAzureSingleResourceGroupDedicatedStorageAccount true", "enableAzureSingleResourceGroupDedicatedStorageAccount", true,
                        "CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT", true},

                {"enableCloudIdentityMappinng false", "enableCloudIdentityMappinng", false, "CDP_CLOUD_IDENTITY_MAPPING", false},
                {"enableCloudIdentityMappinng true", "enableCloudIdentityMappinng", true, "CDP_CLOUD_IDENTITY_MAPPING", true},

                {"enableInternalRepositoryForUpgrade false", "enableInternalRepositoryForUpgrade", false, "CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE", false},
                {"enableInternalRepositoryForUpgrade true", "enableInternalRepositoryForUpgrade", true, "CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE", true},

                {"enableHbaseCloudStorage false", "enableHbaseCloudStorage", false, "CDP_SDX_HBASE_CLOUD_STORAGE", false},
                {"enableHbaseCloudStorage true", "enableHbaseCloudStorage", true, "CDP_SDX_HBASE_CLOUD_STORAGE", true},

                {"enableDataLakeEfs false", "enableDataLakeEfs", false, "CDP_DATA_LAKE_AWS_EFS", false},
                {"enableDataLakeEfs true", "enableDataLakeEfs", true, "CDP_DATA_LAKE_AWS_EFS", true},

                {"enableDataLakeCustomImage false", "enableDataLakeCustomImage", false, "CDP_DATA_LAKE_CUSTOM_IMAGE", false},
                {"enableDataLakeCustomImage true", "enableDataLakeCustomImage", true, "CDP_DATA_LAKE_CUSTOM_IMAGE", true},

                {"enableDatabaseWireEncryption false", "enableDatabaseWireEncryption", false, "CDP_CB_DATABASE_WIRE_ENCRYPTION", false},
                {"enableDatabaseWireEncryption true", "enableDatabaseWireEncryption", true, "CDP_CB_DATABASE_WIRE_ENCRYPTION", true},

                {"datalakeLoadBalancerEnabled false", "datalakeLoadBalancerEnabled", false, "CDP_DATA_LAKE_LOAD_BALANCER", false},
                {"datalakeLoadBalancerEnabled true", "datalakeLoadBalancerEnabled", true, "CDP_DATA_LAKE_LOAD_BALANCER", true},

                {"publicEndpointAccessGatewayEnabled false", "publicEndpointAccessGatewayEnabled", false, "CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY", false},
                {"publicEndpointAccessGatewayEnabled true", "publicEndpointAccessGatewayEnabled", true, "CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY", true},

                {"enableAzureDiskSSEWithCMK false", "enableAzureDiskSSEWithCMK", false, "CDP_CB_AZURE_DISK_SSE_WITH_CMK", false},
                {"enableAzureDiskSSEWithCMK true", "enableAzureDiskSSEWithCMK", true, "CDP_CB_AZURE_DISK_SSE_WITH_CMK", true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("conditionalEntitlementDataProvider")
    void getAccountTestIncludesConditionalEntitlement(String testCaseName, String conditionFieldName, boolean condition, String entitlementName,
            boolean entitlementPresentExpected) {
        ReflectionTestUtils.setField(underTest, "cbLicense", VALID_LICENSE);
        underTest.initializeWorkloadPasswordPolicy();
        ReflectionTestUtils.setField(underTest, conditionFieldName, condition);

        GetAccountRequest req = GetAccountRequest.getDefaultInstance();
        StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();

        underTest.getAccount(req, observer);

        assertThat(observer.getValues().size()).isEqualTo(1);
        GetAccountResponse res = observer.getValues().get(0);
        assertThat(res.hasAccount()).isTrue();
        Account account = res.getAccount();
        List<String> entitlements = account.getEntitlementsList().stream().map(Entitlement::getEntitlementName).collect(Collectors.toList());
        if (entitlementPresentExpected) {
            assertThat(entitlements).contains(entitlementName);
        } else {
            assertThat(entitlements).doesNotContain(entitlementName);
        }
    }

    @Test
    void getAccountTestIncludesIds() {
        ReflectionTestUtils.setField(underTest, "cbLicense", VALID_LICENSE);
        underTest.initializeWorkloadPasswordPolicy();

        GetAccountRequest req = GetAccountRequest.newBuilder()
                .setAccountId(ACCOUNT_ID)
                .build();
        StreamRecorder<GetAccountResponse> observer = StreamRecorder.create();

        underTest.getAccount(req, observer);

        assertThat(observer.getValues().size()).isEqualTo(1);
        GetAccountResponse res = observer.getValues().get(0);
        assertThat(res.hasAccount()).isTrue();
        Account account = res.getAccount();
        assertThat(account.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(account.getExternalAccountId()).isEqualTo("external-" + ACCOUNT_ID);
    }

    @Test
    void testGetAccountWithAltusAccountId() {
        GetAccountRequest req = GetAccountRequest.newBuilder()
                .setAccountId(INTERNAL_ACCOUNT)
                .build();

        expectedException.expect(StatusRuntimeException.class);
        expectedException.expectMessage("INVALID_ARGUMENT");

        underTest.getAccount(req, null);
    }

    @Test
    void listTermsTest() {
        ListTermsRequest req = ListTermsRequest.getDefaultInstance();
        StreamRecorder<ListTermsResponse> observer = StreamRecorder.create();

        underTest.listTerms(req, observer);

        assertThat(observer.getValues().size()).isEqualTo(1);
    }

}