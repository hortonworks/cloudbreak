package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_AUTOMATIC_USERSYNC_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_AZURE_SINGLE_RESOURCE_GROUP;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_CB_FAST_EBS_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_FMS_CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_FREEIPA_DL_EBS_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_FREEIPA_HA;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_FREEIPA_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_FREEIPA_HEALTH_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_RAZ;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_RUNTIME_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CDP_UMS_USER_SYNC_MODEL_GENERATION;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.CLOUDERA_INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.EntitlementService.LOCAL_DEV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Entitlement;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final Account ACCOUNT_NO_ENTITLEMENTS = Account.newBuilder().build();

    private static final String ENTITLEMENT_FOO = "FOO";

    private static final String ENTITLEMENT_BAR = "BAR";

    private static final Account ACCOUNT_ENTITLEMENTS_FOO_BAR = createAccountForEntitlements(ENTITLEMENT_FOO, ENTITLEMENT_BAR);

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private EntitlementService underTest;

    static Object[][] entitlementCheckDataProvider() {
        return new Object[][]{
                // testCaseName, entitlementName, function, enabled
                {"CDP_AZURE == false", CDP_AZURE, (EntitlementCheckFunction) EntitlementService::azureEnabled, false},
                {"CDP_AZURE == true", CDP_AZURE, (EntitlementCheckFunction) EntitlementService::azureEnabled, true},

                {"CDP_GCP == false", CDP_GCP, (EntitlementCheckFunction) EntitlementService::gcpEnabled, false},
                {"CDP_GCP == true", CDP_GCP, (EntitlementCheckFunction) EntitlementService::gcpEnabled, true},

                {"CDP_BASE_IMAGE == false", CDP_BASE_IMAGE, (EntitlementCheckFunction) EntitlementService::baseImageEnabled, false},
                {"CDP_BASE_IMAGE == true", CDP_BASE_IMAGE, (EntitlementCheckFunction) EntitlementService::baseImageEnabled, true},

                {"CDP_AUTOMATIC_USERSYNC_POLLER == false", CDP_AUTOMATIC_USERSYNC_POLLER,
                        (EntitlementCheckFunction) EntitlementService::automaticUsersyncPollerEnabled, false},
                {"CDP_AUTOMATIC_USERSYNC_POLLER == true", CDP_AUTOMATIC_USERSYNC_POLLER,
                        (EntitlementCheckFunction) EntitlementService::automaticUsersyncPollerEnabled, true},

                {"CDP_FREEIPA_HA == false", CDP_FREEIPA_HA, (EntitlementCheckFunction) EntitlementService::freeIpaHaEnabled, false},
                {"CDP_FREEIPA_HA == true", CDP_FREEIPA_HA, (EntitlementCheckFunction) EntitlementService::freeIpaHaEnabled, true},

                {"CDP_FREEIPA_HA_REPAIR == false", CDP_FREEIPA_HA_REPAIR, (EntitlementCheckFunction) EntitlementService::freeIpaHaRepairEnabled, false},
                {"CDP_FREEIPA_HA_REPAIR == true", CDP_FREEIPA_HA_REPAIR, (EntitlementCheckFunction) EntitlementService::freeIpaHaRepairEnabled, true},

                {"CDP_FREEIPA_HEALTH_CHECK == false", CDP_FREEIPA_HEALTH_CHECK, (EntitlementCheckFunction) EntitlementService::freeIpaHealthCheckEnabled, false},
                {"CDP_FREEIPA_HEALTH_CHECK == true", CDP_FREEIPA_HEALTH_CHECK, (EntitlementCheckFunction) EntitlementService::freeIpaHealthCheckEnabled, true},

                {"CLOUDERA_INTERNAL_ACCOUNT == false", CLOUDERA_INTERNAL_ACCOUNT, (EntitlementCheckFunction) EntitlementService::internalTenant, false},
                {"CLOUDERA_INTERNAL_ACCOUNT == true", CLOUDERA_INTERNAL_ACCOUNT, (EntitlementCheckFunction) EntitlementService::internalTenant, true},

                {"CDP_FMS_CLUSTER_PROXY == false", CDP_FMS_CLUSTER_PROXY, (EntitlementCheckFunction) EntitlementService::fmsClusterProxyEnabled, false},
                {"CDP_FMS_CLUSTER_PROXY == true", CDP_FMS_CLUSTER_PROXY, (EntitlementCheckFunction) EntitlementService::fmsClusterProxyEnabled, true},

                {"CDP_CLOUD_STORAGE_VALIDATION == false", CDP_CLOUD_STORAGE_VALIDATION,
                        (EntitlementCheckFunction) EntitlementService::cloudStorageValidationEnabled, false},
                {"CDP_CLOUD_STORAGE_VALIDATION == true", CDP_CLOUD_STORAGE_VALIDATION,
                        (EntitlementCheckFunction) EntitlementService::cloudStorageValidationEnabled, true},

                {"CDP_RAZ == false", CDP_RAZ, (EntitlementCheckFunction) EntitlementService::razEnabled, false},
                {"CDP_RAZ == true", CDP_RAZ, (EntitlementCheckFunction) EntitlementService::razEnabled, true},

                {"CDP_RUNTIME_UPGRADE == false", CDP_RUNTIME_UPGRADE, (EntitlementCheckFunction) EntitlementService::runtimeUpgradeEnabled, false},
                {"CDP_RUNTIME_UPGRADE == true", CDP_RUNTIME_UPGRADE, (EntitlementCheckFunction) EntitlementService::runtimeUpgradeEnabled, true},

                {"CDP_FREEIPA_DL_EBS_ENCRYPTION == false", CDP_FREEIPA_DL_EBS_ENCRYPTION,
                        (EntitlementCheckFunction) EntitlementService::freeIpaDlEbsEncryptionEnabled, false},
                {"CDP_FREEIPA_DL_EBS_ENCRYPTION == true", CDP_FREEIPA_DL_EBS_ENCRYPTION,
                        (EntitlementCheckFunction) EntitlementService::freeIpaDlEbsEncryptionEnabled, true},

                {"LOCAL_DEV == false", LOCAL_DEV, (EntitlementCheckFunction) EntitlementService::localDevelopment, false},
                {"LOCAL_DEV == true", LOCAL_DEV, (EntitlementCheckFunction) EntitlementService::localDevelopment, true},

                {"CDP_AZURE_SINGLE_RESOURCE_GROUP == false", CDP_AZURE_SINGLE_RESOURCE_GROUP,
                        (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDeploymentEnabled, false},
                {"CDP_AZURE_SINGLE_RESOURCE_GROUP == true", CDP_AZURE_SINGLE_RESOURCE_GROUP,
                        (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDeploymentEnabled, true},

                {"CDP_CB_FAST_EBS_ENCRYPTION == false", CDP_CB_FAST_EBS_ENCRYPTION,
                        (EntitlementCheckFunction) EntitlementService::fastEbsEncryptionEnabled, false},
                {"CDP_CB_FAST_EBS_ENCRYPTION == true", CDP_CB_FAST_EBS_ENCRYPTION,
                        (EntitlementCheckFunction) EntitlementService::fastEbsEncryptionEnabled, true},

                {"CDP_CLOUD_IDENTITY_MAPPING == false", CDP_CLOUD_IDENTITY_MAPPING,
                        (EntitlementCheckFunction) EntitlementService::cloudIdentityMappingEnabled, false},
                {"CDP_CLOUD_IDENTITY_MAPPING == true", CDP_CLOUD_IDENTITY_MAPPING,
                        (EntitlementCheckFunction) EntitlementService::cloudIdentityMappingEnabled, true},
                {"CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE == true", CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE,
                        (EntitlementCheckFunction) EntitlementService::isInternalRepositoryForUpgradeAllowed, true},
                {"CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE == false", CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE,
                        (EntitlementCheckFunction) EntitlementService::isInternalRepositoryForUpgradeAllowed, false},
                {"CDP_UMS_USER_SYNC_MODEL_GENERATION == false", CDP_UMS_USER_SYNC_MODEL_GENERATION,
                        (EntitlementCheckFunction) EntitlementService::umsUserSyncModelGenerationEnabled, false},
                {"CDP_UMS_USER_SYNC_MODEL_GENERATION == true", CDP_UMS_USER_SYNC_MODEL_GENERATION,
                        (EntitlementCheckFunction) EntitlementService::umsUserSyncModelGenerationEnabled, true},
                {"CDP_SDX_HBASE_CLOUD_STORAGE == false", CDP_SDX_HBASE_CLOUD_STORAGE,
                        (EntitlementCheckFunction) EntitlementService::sdxHbaseCloudStorageEnabled, false},
                {"CDP_SDX_HBASE_CLOUD_STORAGE == true", CDP_SDX_HBASE_CLOUD_STORAGE,
                        (EntitlementCheckFunction) EntitlementService::sdxHbaseCloudStorageEnabled, true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entitlementCheckDataProvider")
    void entitlementEnabledTestWhenNoOtherEntitlementsAreGranted(String testCaseName, String entitlementName, EntitlementCheckFunction function,
            boolean enabled) {
        setUpUmsClient(enabled ? createAccountForEntitlements(entitlementName) : ACCOUNT_NO_ENTITLEMENTS);
        assertThat(function.entitlementEnabled(underTest, ACTOR_CRN, ACCOUNT_ID)).isEqualTo(enabled);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entitlementCheckDataProvider")
    void entitlementEnabledTestWhenMultipleOtherEntitlementsAreGranted(String testCaseName, String entitlementName, EntitlementCheckFunction function,
            boolean enabled) {
        setUpUmsClient(enabled ? createAccountForEntitlements(ENTITLEMENT_FOO, entitlementName, ENTITLEMENT_BAR) : ACCOUNT_ENTITLEMENTS_FOO_BAR);
        assertThat(function.entitlementEnabled(underTest, ACTOR_CRN, ACCOUNT_ID)).isEqualTo(enabled);
    }

    @Test
    void getEntitlementsTest() {
        setUpUmsClient(ACCOUNT_ENTITLEMENTS_FOO_BAR);
        assertThat(underTest.getEntitlements(ACTOR_CRN, ACCOUNT_ID)).containsExactly(ENTITLEMENT_FOO, ENTITLEMENT_BAR);
    }

    @SuppressWarnings("unchecked")
    private void setUpUmsClient(Account account) {
        when(umsClient.getAccountDetails(eq(ACTOR_CRN), eq(ACCOUNT_ID), any(Optional.class))).thenReturn(account);
    }

    private static Account createAccountForEntitlements(String... entitlementNames) {
        // Protobuf wrappers are all finals, so cannot be mocked
        Account.Builder builder = Account.newBuilder();
        Arrays.stream(entitlementNames).forEach(entitlementName -> builder.addEntitlements(createEntitlement(entitlementName)));
        return builder.build();
    }

    private static Entitlement createEntitlement(String entitlementName) {
        return Entitlement.newBuilder().setEntitlementName(entitlementName).build();
    }

    @FunctionalInterface
    private interface EntitlementCheckFunction {
        boolean entitlementEnabled(EntitlementService service, String actorCrn, String accountId);
    }

}