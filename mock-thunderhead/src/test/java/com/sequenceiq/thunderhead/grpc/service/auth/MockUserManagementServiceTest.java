package com.sequenceiq.thunderhead.grpc.service.auth;

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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Entitlement;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetAccountResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsRequest;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.WorkloadPasswordPolicy;
import com.sequenceiq.thunderhead.util.JsonUtil;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

import io.grpc.internal.testing.StreamRecorder;

@ExtendWith(ExpectedExceptionSupport.class)
@ExtendWith(MockitoExtension.class)
public class MockUserManagementServiceTest {

    private static final String VALID_LICENSE = "License file content";

    private static final String SAMPLE_SSH_PUBLIC_KEY = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIGHA5cmj5+agIalPxw85jFrrZcSh3dl06ukeqKu6JVQm nobody@example.com";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
                    .setActorCrn(Crn.builder()
                            .setPartition(Crn.Partition.CDP)
                            .setAccountId(UUID.randomUUID().toString())
                            .setService(Crn.Service.IAM)
                            .setResourceType(Crn.ResourceType.USER)
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
            assertThat(account.hasPasswordPolicy()).isTrue();
            WorkloadPasswordPolicy passwordPolicy = account.getPasswordPolicy();
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
                "DATAHUB_AWS_AUTOSCALING", "LOCAL_DEV");
    }

    static Object[][] conditionalEntitlementDataProvider() {
        return new Object[][]{
                // testCaseName conditionFieldName condition entitlementName entitlementPresentExpected
                {"enableBaseImages false", "enableBaseImages", false, "CDP_BASE_IMAGE", false},
                {"enableBaseImages true", "enableBaseImages", true, "CDP_BASE_IMAGE", true},

                {"enableFreeIpaHa false", "enableFreeIpaHa", false, "CDP_FREEIPA_HA", false},
                {"enableFreeIpaHa true", "enableFreeIpaHa", true, "CDP_FREEIPA_HA", true},

                {"enableFreeIpaHaRepair false", "enableFreeIpaHaRepair", false, "CDP_FREEIPA_HA_REPAIR", false},
                {"enableFreeIpaHaRepair true", "enableFreeIpaHaRepair", true, "CDP_FREEIPA_HA_REPAIR", true},

                {"enableCloudStorageValidation false", "enableCloudStorageValidation", false, "CDP_CLOUD_STORAGE_VALIDATION", false},
                {"enableCloudStorageValidation true", "enableCloudStorageValidation", true, "CDP_CLOUD_STORAGE_VALIDATION", true},

                {"runtimeUpgradeEnabled false", "runtimeUpgradeEnabled", false, "CDP_RUNTIME_UPGRADE", false},
                {"runtimeUpgradeEnabled true", "runtimeUpgradeEnabled", true, "CDP_RUNTIME_UPGRADE", true},

                {"razEnabled false", "razEnabled", false, "CDP_RAZ", false},
                {"razEnabled true", "razEnabled", true, "CDP_RAZ", true},

                {"enableFreeIpaDlEbsEncryption false", "enableFreeIpaDlEbsEncryption", false, "CDP_FREEIPA_DL_EBS_ENCRYPTION", false},
                {"enableFreeIpaDlEbsEncryption true", "enableFreeIpaDlEbsEncryption", true, "CDP_FREEIPA_DL_EBS_ENCRYPTION", true},

                {"enableAzureSingleResourceGroupDeployment false", "enableAzureSingleResourceGroupDeployment", false, "CDP_AZURE_SINGLE_RESOURCE_GROUP", false},
                {"enableAzureSingleResourceGroupDeployment true", "enableAzureSingleResourceGroupDeployment", true, "CDP_AZURE_SINGLE_RESOURCE_GROUP", true},

                {"enableFastEbsEncryption false", "enableFastEbsEncryption", false, "CDP_CB_FAST_EBS_ENCRYPTION", false},
                {"enableFastEbsEncryption true", "enableFastEbsEncryption", true, "CDP_CB_FAST_EBS_ENCRYPTION", true},
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

}