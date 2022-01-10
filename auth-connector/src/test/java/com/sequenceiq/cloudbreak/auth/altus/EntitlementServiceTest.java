package com.sequenceiq.cloudbreak.auth.altus;

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

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    private static final String ENTITLEMENT_FOO = "FOO";

    private static final String ENTITLEMENT_BAR = "BAR";

    private static final Account ACCOUNT_ENTITLEMENTS_FOO_BAR = createAccountForEntitlements(ENTITLEMENT_FOO, ENTITLEMENT_BAR);

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private EntitlementService underTest;

    static Object[][] entitlementCheckDataProvider() {
        return new Object[][]{

                // entitlementName, function, enabled
                {"CDP_AZURE", (EntitlementCheckFunction) EntitlementService::azureEnabled, false},
                {"CDP_AZURE", (EntitlementCheckFunction) EntitlementService::azureEnabled, true},

                {"CDP_GCP", (EntitlementCheckFunction) EntitlementService::gcpEnabled, false},
                {"CDP_GCP", (EntitlementCheckFunction) EntitlementService::gcpEnabled, true},

                {"AUDIT_ARCHIVING_GCP", (EntitlementCheckFunction) EntitlementService::gcpAuditEnabled, false},
                {"AUDIT_ARCHIVING_GCP", (EntitlementCheckFunction) EntitlementService::gcpAuditEnabled, true},

                {"CDP_CB_AWS_NATIVE", (EntitlementCheckFunction) EntitlementService::awsNativeEnabled, false},
                {"CDP_CB_AWS_NATIVE", (EntitlementCheckFunction) EntitlementService::awsNativeEnabled, true},

                {"CDP_CB_AWS_NATIVE_DATALAKE", (EntitlementCheckFunction) EntitlementService::awsNativeDataLakeEnabled, false},
                {"CDP_CB_AWS_NATIVE_DATALAKE", (EntitlementCheckFunction) EntitlementService::awsNativeDataLakeEnabled, true},

                {"CDP_CB_AWS_NATIVE_FREEIPA", (EntitlementCheckFunction) EntitlementService::awsNativeFreeIpaEnabled, false},
                {"CDP_CB_AWS_NATIVE_FREEIPA", (EntitlementCheckFunction) EntitlementService::awsNativeFreeIpaEnabled, true},

                {"CDP_BASE_IMAGE", (EntitlementCheckFunction) EntitlementService::baseImageEnabled, false},
                {"CDP_BASE_IMAGE", (EntitlementCheckFunction) EntitlementService::baseImageEnabled, true},

                {"CDP_AUTOMATIC_USERSYNC_POLLER", (EntitlementCheckFunction) EntitlementService::automaticUsersyncPollerEnabled, false},
                {"CDP_AUTOMATIC_USERSYNC_POLLER", (EntitlementCheckFunction) EntitlementService::automaticUsersyncPollerEnabled, true},

                {"CDP_FREEIPA_HA_REPAIR", (EntitlementCheckFunction) EntitlementService::freeIpaHaRepairEnabled, false},
                {"CDP_FREEIPA_HA_REPAIR", (EntitlementCheckFunction) EntitlementService::freeIpaHaRepairEnabled, true},

                {"CLOUDERA_INTERNAL_ACCOUNT", (EntitlementCheckFunction) EntitlementService::internalTenant, false},
                {"CLOUDERA_INTERNAL_ACCOUNT", (EntitlementCheckFunction) EntitlementService::internalTenant, true},

                {"CDP_FMS_CLUSTER_PROXY", (EntitlementCheckFunction) EntitlementService::fmsClusterProxyEnabled, false},
                {"CDP_FMS_CLUSTER_PROXY", (EntitlementCheckFunction) EntitlementService::fmsClusterProxyEnabled, true},

                {"CDP_CLOUD_STORAGE_VALIDATION", (EntitlementCheckFunction) EntitlementService::cloudStorageValidationEnabled, false},
                {"CDP_CLOUD_STORAGE_VALIDATION", (EntitlementCheckFunction) EntitlementService::cloudStorageValidationEnabled, true},

                {"CDP_RAZ", (EntitlementCheckFunction) EntitlementService::razEnabled, false},
                {"CDP_RAZ", (EntitlementCheckFunction) EntitlementService::razEnabled, true},

                {"CDP_RUNTIME_UPGRADE", (EntitlementCheckFunction) EntitlementService::runtimeUpgradeEnabled, false},
                {"CDP_RUNTIME_UPGRADE", (EntitlementCheckFunction) EntitlementService::runtimeUpgradeEnabled, true},

                {"CDP_OS_UPGRADE_DATAHUB", (EntitlementCheckFunction) EntitlementService::datahubOsUpgradeEnabled, false},
                {"CDP_OS_UPGRADE_DATAHUB", (EntitlementCheckFunction) EntitlementService::datahubOsUpgradeEnabled, true},

                {"LOCAL_DEV", (EntitlementCheckFunction) EntitlementService::localDevelopment, false},
                {"LOCAL_DEV", (EntitlementCheckFunction) EntitlementService::localDevelopment, true},

                {"CDP_CCM_V2", (EntitlementCheckFunction) EntitlementService::ccmV2Enabled, false},
                {"CDP_CCM_V2", (EntitlementCheckFunction) EntitlementService::ccmV2Enabled, true},

                {"CDP_CCM_V2_JUMPGATE", (EntitlementCheckFunction) EntitlementService::ccmV2JumpgateEnabled, false},
                {"CDP_CCM_V2_JUMPGATE", (EntitlementCheckFunction) EntitlementService::ccmV2JumpgateEnabled, true},

                {"CDP_CCM_V2_USE_ONE_WAY_TLS", (EntitlementCheckFunction) EntitlementService::ccmV2UseOneWayTls, false},
                {"CDP_CCM_V2_USE_ONE_WAY_TLS", (EntitlementCheckFunction) EntitlementService::ccmV2UseOneWayTls, true},

                {"CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE", (EntitlementCheckFunction) EntitlementService::ccmV1ToV2JumpgateUpgradeEnabled, false},
                {"CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE", (EntitlementCheckFunction) EntitlementService::ccmV1ToV2JumpgateUpgradeEnabled, true},

                {"CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE", (EntitlementCheckFunction) EntitlementService::ccmV2ToV2JumpgateUpgradeEnabled, false},
                {"CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE", (EntitlementCheckFunction) EntitlementService::ccmV2ToV2JumpgateUpgradeEnabled, true},

                {"CDP_AZURE_SINGLE_RESOURCE_GROUP", (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDeploymentEnabled, false},
                {"CDP_AZURE_SINGLE_RESOURCE_GROUP", (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDeploymentEnabled, true},

                {"CDP_CLOUD_IDENTITY_MAPPING", (EntitlementCheckFunction) EntitlementService::cloudIdentityMappingEnabled, false},
                {"CDP_CLOUD_IDENTITY_MAPPING", (EntitlementCheckFunction) EntitlementService::cloudIdentityMappingEnabled, true},

                {"CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE", (EntitlementCheckFunction) EntitlementService::isInternalRepositoryForUpgradeAllowed, true},
                {"CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE", (EntitlementCheckFunction) EntitlementService::isInternalRepositoryForUpgradeAllowed, false},

                {"CDP_SDX_HBASE_CLOUD_STORAGE", (EntitlementCheckFunction) EntitlementService::sdxHbaseCloudStorageEnabled, false},
                {"CDP_SDX_HBASE_CLOUD_STORAGE", (EntitlementCheckFunction) EntitlementService::sdxHbaseCloudStorageEnabled, true},

                {"CDP_DATA_LAKE_AWS_EFS", (EntitlementCheckFunction) EntitlementService::dataLakeEfsEnabled, false},
                {"CDP_DATA_LAKE_AWS_EFS", (EntitlementCheckFunction) EntitlementService::dataLakeEfsEnabled, true},

                {"CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE",
                        (EntitlementCheckFunction) EntitlementService::isDifferentDataHubAndDataLakeVersionAllowed, false},
                {"CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE",
                        (EntitlementCheckFunction) EntitlementService::isDifferentDataHubAndDataLakeVersionAllowed, true},

                {"CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT",
                        (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDedicatedStorageAccountEnabled, false},
                {"CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT",
                        (EntitlementCheckFunction) EntitlementService::azureSingleResourceGroupDedicatedStorageAccountEnabled, true},

                {"DATAHUB_AWS_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::awsAutoScalingEnabled, false},
                {"DATAHUB_AWS_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::awsAutoScalingEnabled, true},

                {"DATAHUB_AZURE_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::azureAutoScalingEnabled, false},
                {"DATAHUB_AZURE_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::azureAutoScalingEnabled, true},

                {"DATAHUB_GCP_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::gcpAutoScalingEnabled, false},
                {"DATAHUB_GCP_AUTOSCALING", (EntitlementCheckFunction) EntitlementService::gcpAutoScalingEnabled, true},

                {"DATAHUB_AWS_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::awsStopStartScalingEnabled, false},
                {"DATAHUB_AWS_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::awsStopStartScalingEnabled, true},

                {"DATAHUB_AZURE_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::azureStopStartScalingEnabled, false},
                {"DATAHUB_AZURE_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::azureStopStartScalingEnabled, true},

                {"DATAHUB_GCP_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::gcpStopStartScalingEnabled, false},
                {"DATAHUB_GCP_STOP_START_SCALING", (EntitlementCheckFunction) EntitlementService::gcpStopStartScalingEnabled, true},

                {"CDP_CB_DATABASE_WIRE_ENCRYPTION", (EntitlementCheckFunction) EntitlementService::databaseWireEncryptionEnabled, false},
                {"CDP_CB_DATABASE_WIRE_ENCRYPTION", (EntitlementCheckFunction) EntitlementService::databaseWireEncryptionEnabled, true},

                {"CDP_DATA_LAKE_LOAD_BALANCER", (EntitlementCheckFunction) EntitlementService::datalakeLoadBalancerEnabled, false},
                {"CDP_DATA_LAKE_LOAD_BALANCER", (EntitlementCheckFunction) EntitlementService::datalakeLoadBalancerEnabled, true},

                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE", (EntitlementCheckFunction) EntitlementService::azureEndpointGatewayEnabled, false},
                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE", (EntitlementCheckFunction) EntitlementService::azureEndpointGatewayEnabled, true},

                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP", (EntitlementCheckFunction) EntitlementService::gcpEndpointGatewayEnabled, false},
                {"CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP", (EntitlementCheckFunction) EntitlementService::gcpEndpointGatewayEnabled, true},

                {"CDP_CB_AZURE_DISK_SSE_WITH_CMK", (EntitlementCheckFunction) EntitlementService::isAzureDiskSSEWithCMKEnabled, false},
                {"CDP_CB_AZURE_DISK_SSE_WITH_CMK", (EntitlementCheckFunction) EntitlementService::isAzureDiskSSEWithCMKEnabled, true},

                {"CDP_CB_AWS_DISK_ENCRYPTION_WITH_CMK", (EntitlementCheckFunction) EntitlementService::isAWSDiskEncryptionWithCMKEnabled, false},
                {"CDP_CB_AWS_DISK_ENCRYPTION_WITH_CMK", (EntitlementCheckFunction) EntitlementService::isAWSDiskEncryptionWithCMKEnabled, true},

                {"CDP_DATAHUB_CUSTOM_CONFIGS", (EntitlementCheckFunction) EntitlementService::datahubCustomConfigsEnabled, false},
                {"CDP_DATAHUB_CUSTOM_CONFIGS", (EntitlementCheckFunction) EntitlementService::datahubCustomConfigsEnabled, true},

                {"CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION",
                        (EntitlementCheckFunction) EntitlementService::usersyncCredentialsUpdateOptimizationEnabled, false},
                {"CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION",
                        (EntitlementCheckFunction) EntitlementService::usersyncCredentialsUpdateOptimizationEnabled, true},

                {"CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION", (EntitlementCheckFunction) EntitlementService::endpointGatewaySkipValidation, false},
                {"CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION", (EntitlementCheckFunction) EntitlementService::endpointGatewaySkipValidation, true},

                {"CDP_CM_HA", (EntitlementCheckFunction) EntitlementService::cmHAEnabled, false},
                {"CDP_CM_HA", (EntitlementCheckFunction) EntitlementService::cmHAEnabled, true},

                {"CDP_AWS_RESTRICTED_POLICY", (EntitlementCheckFunction) EntitlementService::awsRestrictedPolicy, false},
                {"CDP_AWS_RESTRICTED_POLICY", (EntitlementCheckFunction) EntitlementService::awsRestrictedPolicy, true},

                {"CDP_CONCLUSION_CHECKER_SEND_USER_EVENT", (EntitlementCheckFunction) EntitlementService::conclusionCheckerSendUserEventEnabled, false},
                {"CDP_CONCLUSION_CHECKER_SEND_USER_EVENT", (EntitlementCheckFunction) EntitlementService::conclusionCheckerSendUserEventEnabled, true},

                {"CDP_NODESTATUS_ENABLE_SALT_PING", (EntitlementCheckFunction) EntitlementService::nodestatusSaltPingEnabled, false},
                {"CDP_NODESTATUS_ENABLE_SALT_PING", (EntitlementCheckFunction) EntitlementService::nodestatusSaltPingEnabled, true},

                {"EPHEMERAL_DISKS_FOR_TEMP_DATA", (EntitlementCheckFunction) EntitlementService::ephemeralDisksForTempDataEnabled, false},
                {"EPHEMERAL_DISKS_FOR_TEMP_DATA", (EntitlementCheckFunction) EntitlementService::ephemeralDisksForTempDataEnabled, true},

                {"CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK", (EntitlementCheckFunction) EntitlementService::isGcpDiskEncryptionWithCMEKEnabled, false},
                {"CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK", (EntitlementCheckFunction) EntitlementService::isGcpDiskEncryptionWithCMEKEnabled, true}
        };
    }

    @ParameterizedTest(name = "{0} == {2}")
    @MethodSource("entitlementCheckDataProvider")
    void entitlementEnabledTestWhenNoOtherEntitlementsAreGranted(String entitlementName, EntitlementCheckFunction function, boolean enabled) {
        setUpUmsClient(entitlementName, enabled);
        assertThat(function.entitlementEnabled(underTest, ACCOUNT_ID)).isEqualTo(enabled);
    }

    @Test
    void getEntitlementsTest() {
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID), any(Optional.class)))
                .thenReturn(ACCOUNT_ENTITLEMENTS_FOO_BAR);
        assertThat(underTest.getEntitlements(ACCOUNT_ID)).containsExactly(ENTITLEMENT_FOO, ENTITLEMENT_BAR);
    }

    @SuppressWarnings("unchecked")
    private void setUpUmsClient(String entitlement, boolean entitled) {
        Account.Builder builder = Account.newBuilder();
        if (entitled) {
                builder.addEntitlements(
                        Entitlement.newBuilder()
                                .setEntitlementName(entitlement)
                                .build());
        }
        when(umsClient.getAccountDetails(eq(ACCOUNT_ID), any()))
                .thenReturn(builder.build());
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
        boolean entitlementEnabled(EntitlementService service, String accountId);
    }

}
