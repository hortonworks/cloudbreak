package com.sequenceiq.cloudbreak.auth.altus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Entitlement;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENTITLEMENT_FOO = "FOO";

    private static final String ENTITLEMENT_BAR = "BAR";

    private static final Account ACCOUNT_ENTITLEMENTS_FOO_BAR = createAccountForEntitlements(ENTITLEMENT_FOO, ENTITLEMENT_BAR);

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:5678";

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private EntitlementService underTest;

    static Object[][] entitlementCheckDataProvider() {
        return new Object[][]{

                // entitlementName, function, enabled

                {"CDP_BASE_IMAGE", (EntitlementCheckFunction) EntitlementService::baseImageEnabled, false},
                {"CDP_BASE_IMAGE", (EntitlementCheckFunction) EntitlementService::baseImageEnabled, true},

                {"CDP_FREEIPA_REBUILD", (EntitlementCheckFunction) EntitlementService::isFreeIpaRebuildEnabled, false},
                {"CDP_FREEIPA_REBUILD", (EntitlementCheckFunction) EntitlementService::isFreeIpaRebuildEnabled, true},

                {"CDP_FREEIPA_LOAD_BALANCER", (EntitlementCheckFunction) EntitlementService::isFreeIpaLoadBalancerEnabled, false},
                {"CDP_FREEIPA_LOAD_BALANCER", (EntitlementCheckFunction) EntitlementService::isFreeIpaLoadBalancerEnabled, true},

                {"CLOUDERA_INTERNAL_ACCOUNT", (EntitlementCheckFunction) EntitlementService::internalTenant, false},
                {"CLOUDERA_INTERNAL_ACCOUNT", (EntitlementCheckFunction) EntitlementService::internalTenant, true},

                {"CDP_CLOUD_STORAGE_VALIDATION", (EntitlementCheckFunction) EntitlementService::cloudStorageValidationEnabled, false},
                {"CDP_CLOUD_STORAGE_VALIDATION", (EntitlementCheckFunction) EntitlementService::cloudStorageValidationEnabled, true},

                {"LOCAL_DEV", (EntitlementCheckFunction) EntitlementService::localDevelopment, false},
                {"LOCAL_DEV", (EntitlementCheckFunction) EntitlementService::localDevelopment, true},

                {"CDP_CLOUD_IDENTITY_MAPPING", (EntitlementCheckFunction) EntitlementService::cloudIdentityMappingEnabled, false},
                {"CDP_CLOUD_IDENTITY_MAPPING", (EntitlementCheckFunction) EntitlementService::cloudIdentityMappingEnabled, true},

                {"CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE", (EntitlementCheckFunction) EntitlementService::isInternalRepositoryForUpgradeAllowed, true},
                {"CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE", (EntitlementCheckFunction) EntitlementService::isInternalRepositoryForUpgradeAllowed, false},

                {"CDP_SDX_HBASE_CLOUD_STORAGE", (EntitlementCheckFunction) EntitlementService::sdxHbaseCloudStorageEnabled, false},
                {"CDP_SDX_HBASE_CLOUD_STORAGE", (EntitlementCheckFunction) EntitlementService::sdxHbaseCloudStorageEnabled, true},

                {"CDP_TRIAL", (EntitlementCheckFunction) EntitlementService::cdpTrialEnabled, false},
                {"CDP_TRIAL", (EntitlementCheckFunction) EntitlementService::cdpTrialEnabled, true},

                {"CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE",
                        (EntitlementCheckFunction) EntitlementService::isDifferentDataHubAndDataLakeVersionAllowed, false},
                {"CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE",
                        (EntitlementCheckFunction) EntitlementService::isDifferentDataHubAndDataLakeVersionAllowed, true},

                {"CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT",
                        (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDedicatedStorageAccountEnabled, false},
                {"CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT",
                        (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDedicatedStorageAccountEnabled, true},

                {"DATAHUB_GCP_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::gcpAutoScalingEnabled, false},
                {"DATAHUB_GCP_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::gcpAutoScalingEnabled, true},

                {"DATAHUB_AWS_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::awsStopStartScalingEnabled, false},
                {"DATAHUB_AWS_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::awsStopStartScalingEnabled, true},

                {"DATAHUB_AZURE_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::azureStopStartScalingEnabled, false},
                {"DATAHUB_AZURE_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::azureStopStartScalingEnabled, true},

                {"DATAHUB_GCP_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::gcpStopStartScalingEnabled, false},
                {"DATAHUB_GCP_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::gcpStopStartScalingEnabled, true},

                {"DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY",
                        (EntitlementCheckFunction) EntitlementService::stopStartScalingFailureRecoveryEnabled, false},
                {"DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY",
                        (EntitlementCheckFunction) EntitlementService::stopStartScalingFailureRecoveryEnabled, true},

                {"CDP_DATA_LAKE_LOAD_BALANCER", (EntitlementCheckFunction) EntitlementService::datalakeLoadBalancerEnabled, false},
                {"CDP_DATA_LAKE_LOAD_BALANCER", (EntitlementCheckFunction) EntitlementService::datalakeLoadBalancerEnabled, true},

                {"CDP_DATALAKE_RESIZE_RECOVERY", (EntitlementCheckFunction) EntitlementService::isDatalakeResizeRecoveryEnabled, false},
                {"CDP_DATALAKE_RESIZE_RECOVERY", (EntitlementCheckFunction) EntitlementService::isDatalakeResizeRecoveryEnabled, true},

                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE", (EntitlementCheckFunction) EntitlementService::azureEndpointGatewayEnabled, false},
                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE", (EntitlementCheckFunction) EntitlementService::azureEndpointGatewayEnabled, true},

                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP", (EntitlementCheckFunction) EntitlementService::gcpEndpointGatewayEnabled, false},
                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP", (EntitlementCheckFunction) EntitlementService::gcpEndpointGatewayEnabled, true},

                {"CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION",
                        (EntitlementCheckFunction) EntitlementService::usersyncCredentialsUpdateOptimizationEnabled, false},
                {"CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION",
                        (EntitlementCheckFunction) EntitlementService::usersyncCredentialsUpdateOptimizationEnabled, true},

                {"CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION", (EntitlementCheckFunction) EntitlementService::endpointGatewaySkipValidation, false},
                {"CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION", (EntitlementCheckFunction) EntitlementService::endpointGatewaySkipValidation, true},

                {"CDP_AWS_RESTRICTED_POLICY", (EntitlementCheckFunction) EntitlementService::awsRestrictedPolicy, false},
                {"CDP_AWS_RESTRICTED_POLICY", (EntitlementCheckFunction) EntitlementService::awsRestrictedPolicy, true},

                {"CDP_CONCLUSION_CHECKER_SEND_USER_EVENT", (EntitlementCheckFunction) EntitlementService::conclusionCheckerSendUserEventEnabled, false},
                {"CDP_CONCLUSION_CHECKER_SEND_USER_EVENT", (EntitlementCheckFunction) EntitlementService::conclusionCheckerSendUserEventEnabled, true},

                {"E2E_TEST_ONLY", (EntitlementCheckFunction) EntitlementService::isE2ETestOnlyEnabled, false},
                {"E2E_TEST_ONLY", (EntitlementCheckFunction) EntitlementService::isE2ETestOnlyEnabled, true},

                {"WORKLOAD_IAM_SYNC", (EntitlementCheckFunction) EntitlementService::isWorkloadIamSyncEnabled, false},
                {"WORKLOAD_IAM_SYNC", (EntitlementCheckFunction) EntitlementService::isWorkloadIamSyncEnabled, true},

                {"CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT", (EntitlementCheckFunction) EntitlementService::isUserSyncEnforceGroupMembershipLimitEnabled, false},
                {"CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT", (EntitlementCheckFunction) EntitlementService::isUserSyncEnforceGroupMembershipLimitEnabled, true},

                {"CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL", (EntitlementCheckFunction) EntitlementService::isUserSyncSplitFreeIPAUserRetrievalEnabled, false},
                {"CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL", (EntitlementCheckFunction) EntitlementService::isUserSyncSplitFreeIPAUserRetrievalEnabled, true},

                {"TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY",
                        (EntitlementCheckFunction) EntitlementService::isTargetingSubnetsForEndpointAccessGatewayEnabled, false},
                {"TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY",
                        (EntitlementCheckFunction) EntitlementService::isTargetingSubnetsForEndpointAccessGatewayEnabled, true},

                {"CDP_AZURE_IMAGE_MARKETPLACE_ONLY", (EntitlementCheckFunction) EntitlementService::azureOnlyMarketplaceImagesEnabled, false},
                {"CDP_AZURE_IMAGE_MARKETPLACE_ONLY", (EntitlementCheckFunction) EntitlementService::azureOnlyMarketplaceImagesEnabled, true},

                {"WORKLOAD_IAM_USERSYNC_ROUTING", (EntitlementCheckFunction) EntitlementService::isWiamUsersyncRoutingEnabled, false},
                {"WORKLOAD_IAM_USERSYNC_ROUTING", (EntitlementCheckFunction) EntitlementService::isWiamUsersyncRoutingEnabled, true},

                {"CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED", (EntitlementCheckFunction) EntitlementService::isFedRampExternalDatabaseForceDisabled, false},
                {"CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED", (EntitlementCheckFunction) EntitlementService::isFedRampExternalDatabaseForceDisabled, true},

                {"CDP_CB_AZURE_MULTIAZ", (EntitlementCheckFunction) EntitlementService::isAzureMultiAzEnabled, false},
                {"CDP_CB_AZURE_MULTIAZ", (EntitlementCheckFunction) EntitlementService::isAzureMultiAzEnabled, true},

                {"CDP_CB_GCP_MULTIAZ", (EntitlementCheckFunction) EntitlementService::isGcpMultiAzEnabled, false},
                {"CDP_CB_GCP_MULTIAZ", (EntitlementCheckFunction) EntitlementService::isGcpMultiAzEnabled, true},

                {"CDP_CB_SECRET_ENCRYPTION", (EntitlementCheckFunction) EntitlementService::isSecretEncryptionEnabled, false},
                {"CDP_CB_SECRET_ENCRYPTION", (EntitlementCheckFunction) EntitlementService::isSecretEncryptionEnabled, true},

                {"CDP_CB_SECRET_ENCRYPTION", (EntitlementCheckFunction) EntitlementService::isSecretEncryptionEnabled, false},
                {"CDP_CB_SECRET_ENCRYPTION", (EntitlementCheckFunction) EntitlementService::isSecretEncryptionEnabled, true},

                {"CDP_EXPRESS_ONBOARDING", (EntitlementCheckFunction) EntitlementService::isExpressOnboardingEnabled, false},
                {"CDP_EXPRESS_ONBOARDING", (EntitlementCheckFunction) EntitlementService::isExpressOnboardingEnabled, true},

                {"CDP_SECURITY_ENFORCING_SELINUX", (EntitlementCheckFunction) EntitlementService::isCdpSecurityEnforcingSELinux, false},
                {"CDP_SECURITY_ENFORCING_SELINUX", (EntitlementCheckFunction) EntitlementService::isCdpSecurityEnforcingSELinux, true},

                {"CDP_CB_GCP_SECURE_BOOT", (EntitlementCheckFunction) EntitlementService::isGcpSecureBootEnabled, false},
                {"CDP_CB_GCP_SECURE_BOOT", (EntitlementCheckFunction) EntitlementService::isGcpSecureBootEnabled, true},

                {"CDP_DATAHUB_FORCE_OS_UPGRADE", (EntitlementCheckFunction) EntitlementService::isDatahubForceOsUpgradeEnabled, false},
                {"CDP_DATAHUB_FORCE_OS_UPGRADE", (EntitlementCheckFunction) EntitlementService::isDatahubForceOsUpgradeEnabled, true},

                {"CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE_LONG_POLLING",
                        (EntitlementCheckFunction) EntitlementService::isFlexibleServerUpgradeLongPollingEnabled, false},
                {"CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE_LONG_POLLING",
                        (EntitlementCheckFunction) EntitlementService::isFlexibleServerUpgradeLongPollingEnabled, true},

                {"CDP_AZURE_DATABASE_SINGLE_SERVER_REJECT", (EntitlementCheckFunction) EntitlementService::isSingleServerRejectEnabled, false},
                {"CDP_AZURE_DATABASE_SINGLE_SERVER_REJECT", (EntitlementCheckFunction) EntitlementService::isSingleServerRejectEnabled, true},

                {"CDP_LAKEHOUSE_OPTIMIZER_ENABLED", (EntitlementCheckFunction) EntitlementService::isLakehouseOptimizerEnabled, false},
                {"CDP_LAKEHOUSE_OPTIMIZER_ENABLED", (EntitlementCheckFunction) EntitlementService::isLakehouseOptimizerEnabled, true},

                {"CDP_CB_CONFIGURE_ENCRYPTION_PROFILE", (EntitlementCheckFunction) EntitlementService::isConfigureEncryptionProfileEnabled, false},
                {"CDP_CB_CONFIGURE_ENCRYPTION_PROFILE", (EntitlementCheckFunction) EntitlementService::isConfigureEncryptionProfileEnabled, true},

                {"CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION", (EntitlementCheckFunction) EntitlementService::isZookeeperToKRaftMigrationEnabled, false},
                {"CDP_ENABLE_ZOOKEEPER_TO_KRAFT_MIGRATION", (EntitlementCheckFunction) EntitlementService::isZookeeperToKRaftMigrationEnabled, true},

                {"CDP_MITIGATE_RELEASE_FAILURE_7218P1100", (EntitlementCheckFunction) EntitlementService::isMitigateReleaseFailure7218P1100Enabled, false},
                {"CDP_MITIGATE_RELEASE_FAILURE_7218P1100", (EntitlementCheckFunction) EntitlementService::isMitigateReleaseFailure7218P1100Enabled, true},

                {"CDP_CB_SUPPORTS_TLS_1_3_ONLY", (EntitlementCheckFunction) EntitlementService::isTlsv13OnlyEnabled, false},
                {"CDP_CB_SUPPORTS_TLS_1_3_ONLY", (EntitlementCheckFunction) EntitlementService::isTlsv13OnlyEnabled, true},

                {"CDP_CB_VERTICAL_SCALE_HA", (EntitlementCheckFunction) EntitlementService::isVerticalScaleHaEnabled, false},
                {"CDP_CB_VERTICAL_SCALE_HA", (EntitlementCheckFunction) EntitlementService::isVerticalScaleHaEnabled, true},
        };
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

    @ParameterizedTest(name = "{0} == {2}")
    @MethodSource("entitlementCheckDataProvider")
    void entitlementEnabledTestWhenNoOtherEntitlementsAreGranted(String entitlementName, EntitlementCheckFunction function, boolean enabled) {
        setUpUmsClient(enabled, entitlementName);
        assertThat(function.isEntitlementEnabled(underTest, ACCOUNT_ID)).isEqualTo(enabled);
    }

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void testAzureDeleteDiskEnabled(boolean entitled) {
        setUpUmsClient(entitled, "CDP_CB_AZURE_DELETE_DISK", "CDP_CB_AZURE_DELETE_DISK");
        assertEquals(entitled, underTest.azureDeleteDiskEnabled(ACCOUNT_ID));
    }

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void testAzureAddDiskEnabled(boolean entitled) {
        setUpUmsClient(entitled, "CDP_CB_AZURE_ADD_DISK", "CDP_CB_AZURE_ADD_DISK");
        assertEquals(entitled, underTest.azureAddDiskEnabled(ACCOUNT_ID));
    }

    @Test
    void getEntitlementsTest() {
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID))).thenReturn(ACCOUNT_ENTITLEMENTS_FOO_BAR);
        assertThat(underTest.getEntitlements(ACCOUNT_ID)).containsExactly(ENTITLEMENT_FOO, ENTITLEMENT_BAR);
    }

    @Test
    void testGetEntitlementsWithCrnInsteadOfAccountId() {
        assertThrows(IllegalArgumentException.class, () -> underTest.getEntitlements(USER_CRN));
    }

    @Test
    void testEntitlementCheckWithCrnInsteadOfAccountId() {
        assertThrows(IllegalArgumentException.class, () -> underTest.isSingleServerRejectEnabled(USER_CRN));
    }

    private void setUpUmsClient(boolean entitled, String... entitlements) {
        Account.Builder builder = Account.newBuilder();
        if (entitled) {
            Arrays.stream(entitlements)
                    .map(EntitlementServiceTest::createEntitlement)
                    .forEach(builder::addEntitlements);
        }
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID))).thenReturn(builder.build());
    }

    @FunctionalInterface
    private interface EntitlementCheckFunction {
        boolean isEntitlementEnabled(EntitlementService service, String accountId);
    }

}
