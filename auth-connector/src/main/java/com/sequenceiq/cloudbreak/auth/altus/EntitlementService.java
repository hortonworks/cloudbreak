package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.ACCESS_KEY_ECDSA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.AUDIT_ARCHIVING_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AWS_RESTRICTED_POLICY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_CERTIFICATE_AUTH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_FREEIPA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_VARIANT_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_ENCRYPTION_AT_HOST;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_VERTICAL_SCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_CO2_CALCULATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_COST_CALCULATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_DATABASE_WIRE_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_DATABASE_WIRE_ENCRYPTION_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_FREEIPA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_VERTICAL_SCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_JUMPGATE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_USE_ONE_WAY_TLS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CENTRAL_COMPUTE_MONITORING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AWS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_ON_VM;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_BULK_HOSTS_REMOVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_DISABLE_AUTO_BUNDLE_COLLECTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_HA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CONCLUSION_CHECKER_SEND_USER_EVENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_DATABUS_ENDPOINT_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_EXPERIMENTAL_SCALE_LIMITS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_NODESTATUS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_LONG_TIMEOUT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_RESIZE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_RESIZE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_SELECT_INSTANCE_TYPE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_ZDU_OS_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_AWS_EFS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_BACKUP_RESTORE_PERMISSION_CHECKS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_DISTROX_INSTANCE_TYPES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENVIRONMENT_EDIT_PROXY_CONFIG;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENVIRONMENT_PRIVILEGED_USER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_DELAYED_STOP_START;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_USERSYNC_THREAD_TIMEOUT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_DATABUS_ENDPOINT_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_REBUILD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_SELECT_INSTANCE_TYPE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MICRO_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_NODESTATUS_ENABLE_SALT_PING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_OS_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_EMBEDDED;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_EXCEPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ROTATE_SALTUSER_PASSWORD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS_SDX_INTEGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_TARGETED_UPSCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UNBOUND_ELIMINATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USE_CM_SYNC_COMMAND_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USE_DATABUS_CNAME_ENDPOINT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_VM_DIAGNOSTICS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CLOUDERA_INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.E2E_TEST_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.EPHEMERAL_DISKS_FOR_TEMP_DATA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.FMS_FREEIPA_BATCH_CALL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.LOCAL_DEV;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OJDBC_TOKEN_DH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OJDBC_TOKEN_DH_ONE_HOUR_TOKEN;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.PERSONAL_VIEW_CB_BY_RIGHT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.SDX_CONFIGURATION_OPTIMIZATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.UI_EDP_PROGRESS_BAR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_SYNC;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_USERSYNC_ROUTING;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@Service
public class EntitlementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public boolean isEntitledFor(String accountId, Entitlement entitledFor) {
        return isEntitlementRegistered(accountId, entitledFor);
    }

    public boolean isEntitledForVirtualGroupRight(String accountId, UmsVirtualGroupRight right) {
        return right.getEntitlement().isEmpty() || isEntitlementRegistered(accountId, right.getEntitlement().get());
    }

    public boolean isFmsToFreeipaBatchCallEnabled(String accountId) {
        return isEntitlementRegistered(accountId, FMS_FREEIPA_BATCH_CALL);
    }

    public boolean listFilteringEnabled(String accountId) {
        return isEntitlementRegistered(accountId, PERSONAL_VIEW_CB_BY_RIGHT);
    }

    public boolean azureEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE);
    }

    public boolean gcpAuditEnabled(String accountId) {
        return isEntitlementRegistered(accountId, AUDIT_ARCHIVING_GCP);
    }

    public boolean enforceAwsNativeForSingleAzFreeipaEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_FREEIPA);
    }

    public boolean enforceAwsNativeForSingleAzDatahubEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATAHUB);
    }

    public boolean enforceAwsNativeForSingleAzDatalakeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATALAKE);
    }

    public boolean awsNativeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_NATIVE);
    }

    public boolean awsVariantMigrationEnable(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_VARIANT_MIGRATION);
    }

    public boolean awsNativeDataLakeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_NATIVE_DATALAKE);
    }

    public boolean awsNativeFreeIpaEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_NATIVE_FREEIPA);
    }

    public boolean baseImageEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_BASE_IMAGE);
    }

    public boolean useDataBusCNameEndpointEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_USE_DATABUS_CNAME_ENDPOINT);
    }

    public boolean useCmSyncCommandPoller(String accountId) {
        return isEntitlementRegistered(accountId, CDP_USE_CM_SYNC_COMMAND_POLLER);
    }

    public boolean cmAutoBundleCollectionDisabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CM_DISABLE_AUTO_BUNDLE_COLLECTION);
    }

    public boolean datahubNodestatusCheckEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_NODESTATUS_CHECK);
    }

    public boolean nodestatusSaltPingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_NODESTATUS_ENABLE_SALT_PING);
    }

    public boolean isCdpSaasEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SAAS);
    }

    public boolean isSdxSaasIntegrationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SAAS_SDX_INTEGRATION);
    }

    public boolean enableDistroxInstanceTypesEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ENABLE_DISTROX_INSTANCE_TYPES);
    }

    public boolean freeIpaHaRepairEnabled(String accountID) {
        return isEntitlementRegistered(accountID, CDP_FREEIPA_HA_REPAIR);
    }

    public boolean isFreeIpaRebuildEnabled(String accountID) {
        return isEntitlementRegistered(accountID, CDP_FREEIPA_REBUILD);
    }

    public boolean internalTenant(String accountId) {
        return isEntitlementRegistered(accountId, CLOUDERA_INTERNAL_ACCOUNT);
    }

    public boolean localDevelopment(String accountId) {
        return isEntitlementRegistered(accountId, LOCAL_DEV);
    }

    public boolean fmsClusterProxyEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FMS_CLUSTER_PROXY);
    }

    public boolean cloudStorageValidationOnVmEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CLOUD_STORAGE_VALIDATION_ON_VM);
    }

    public boolean cloudStorageValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CLOUD_STORAGE_VALIDATION);
    }

    public boolean awsCloudStorageValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CLOUD_STORAGE_VALIDATION_AWS);
    }

    public boolean azureCloudStorageValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CLOUD_STORAGE_VALIDATION_AZURE);
    }

    public boolean gcpCloudStorageValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CLOUD_STORAGE_VALIDATION_GCP);
    }

    public boolean microDutySdxEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_MICRO_DUTY_SDX);
    }

    public boolean runtimeUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_RUNTIME_UPGRADE);
    }

    public boolean datahubRuntimeUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_RUNTIME_UPGRADE_DATAHUB);
    }

    public boolean datahubOsUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_OS_UPGRADE_DATAHUB);
    }

    public boolean azureSingleResourceGroupDeploymentEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_SINGLE_RESOURCE_GROUP);
    }

    public boolean azureMarketplaceImagesEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_IMAGE_MARKETPLACE);
    }

    public boolean azureOnlyMarketplaceImagesEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_IMAGE_MARKETPLACE_ONLY);
    }

    public boolean azureSingleResourceGroupDedicatedStorageAccountEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT);
    }

    public boolean cloudIdentityMappingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CLOUD_IDENTITY_MAPPING);
    }

    public boolean isInternalRepositoryForUpgradeAllowed(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE);
    }

    public boolean isDifferentDataHubAndDataLakeVersionAllowed(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE);
    }

    public boolean sdxHbaseCloudStorageEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SDX_HBASE_CLOUD_STORAGE);
    }

    public boolean dataLakeEfsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_AWS_EFS);
    }

    public boolean awsAutoScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_AWS_AUTOSCALING);
    }

    public boolean awsStopStartScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_AWS_STOP_START_SCALING);
    }

    public boolean azureAutoScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_AZURE_AUTOSCALING);
    }

    public boolean azureStopStartScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_AZURE_STOP_START_SCALING);
    }

    public boolean gcpAutoScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_GCP_AUTOSCALING);
    }

    public boolean stopStartScalingFailureRecoveryEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY);
    }

    public boolean gcpStopStartScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_GCP_STOP_START_SCALING);
    }

    public boolean ccmV2Enabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V2);
    }

    public boolean ccmV2JumpgateEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V2_JUMPGATE);
    }

    public boolean azureVerticalScaleEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_VERTICAL_SCALE);
    }

    public boolean gcpVerticalScaleEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_GCP_VERTICAL_SCALE);
    }

    public boolean ccmV1ToV2JumpgateUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE);
    }

    public boolean ccmV2ToV2JumpgateUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE);
    }

    public boolean ccmV2UseOneWayTls(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V2_USE_ONE_WAY_TLS);
    }

    public boolean databaseWireEncryptionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_DATABASE_WIRE_ENCRYPTION);
    }

    public boolean databaseWireEncryptionDatahubEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_DATABASE_WIRE_ENCRYPTION_DATAHUB);
    }

    public boolean datalakeLoadBalancerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_LOAD_BALANCER);
    }

    public boolean azureDatalakeLoadBalancerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_LOAD_BALANCER_AZURE);
    }

    public boolean azureEndpointGatewayEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE);
    }

    public boolean gcpEndpointGatewayEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP);
    }

    public boolean isDatalakeBackupOnUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_BACKUP_ON_UPGRADE);
    }

    public boolean isDatalakeBackupOnResizeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_BACKUP_ON_RESIZE);
    }

    public boolean isDatalakeBackupRestorePrechecksEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_BACKUP_RESTORE_PERMISSION_CHECKS);
    }

    public boolean isDatalakeResizeRecoveryEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_RESIZE_RECOVERY);
    }

    public boolean isDatalakeLightToMediumMigrationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION);
    }

    public boolean isExperienceDeletionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT);
    }

    public boolean isAzureEncryptionAtHostEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_ENCRYPTION_AT_HOST);
    }

    public boolean isDiagnosticsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_VM_DIAGNOSTICS);
    }

    public boolean isFreeIpaDatabusEndpointValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FREEIPA_DATABUS_ENDPOINT_VALIDATION);
    }

    public boolean isDatahubDatabusEndpointValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_DATABUS_ENDPOINT_VALIDATION);
    }

    public boolean isEDPProgressBarEnabled(String accountID) {
        return isEntitlementRegistered(accountID, UI_EDP_PROGRESS_BAR);
    }

    public boolean usersyncCredentialsUpdateOptimizationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION);
    }

    public boolean endpointGatewaySkipValidation(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION);
    }

    public boolean awsRestrictedPolicy(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AWS_RESTRICTED_POLICY);
    }

    public boolean cmHAEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CM_HA);
    }

    public boolean haRepairEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ALLOW_HA_REPAIR);
    }

    public boolean haUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ALLOW_HA_UPGRADE);
    }

    public boolean conclusionCheckerSendUserEventEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CONCLUSION_CHECKER_SEND_USER_EVENT);
    }

    public boolean ephemeralDisksForTempDataEnabled(String accountId) {
        return isEntitlementRegistered(accountId, EPHEMERAL_DISKS_FOR_TEMP_DATA);
    }

    public boolean isFreeIpaUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FREEIPA_UPGRADE);
    }

    public boolean isDatalakeMediumDutyWithProfilerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER);
    }

    public boolean isUnboundEliminationSupported(String accountId) {
        return isEntitlementRegistered(accountId, CDP_UNBOUND_ELIMINATION);
    }

    public boolean bulkHostsRemovalFromCMSupported(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CM_BULK_HOSTS_REMOVAL);
    }

    public boolean isOjdbcTokenDh(String accountId) {
        return isEntitlementRegistered(accountId, OJDBC_TOKEN_DH);
    }

    public boolean isOjdbcTokenDhOneHour(String accountId) {
        return isEntitlementRegistered(accountId, OJDBC_TOKEN_DH_ONE_HOUR_TOKEN);
    }

    public boolean isE2ETestOnlyEnabled(String accountId) {
        return isEntitlementRegistered(accountId, E2E_TEST_ONLY);
    }

    public boolean targetedUpscaleSupported(String accountId) {
        return isEntitlementRegistered(accountId, CDP_TARGETED_UPSCALE);
    }

    public boolean isDatalakeSelectInstanceTypeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_SELECT_INSTANCE_TYPE);
    }

    public boolean isFreeIpaSelectInstanceTypeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FREEIPA_SELECT_INSTANCE_TYPE);
    }

    public boolean isDatalakeZduOSUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_ZDU_OS_UPGRADE);
    }

    public boolean isEnvironmentPrivilegedUserEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ENVIRONMENT_PRIVILEGED_USER);
    }

    public boolean isComputeMonitoringEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CENTRAL_COMPUTE_MONITORING);
    }

    public boolean isAzureCertificateAuthEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_CERTIFICATE_AUTH);
    }

    public boolean isWorkloadIamSyncEnabled(String accountId) {
        return isEntitlementRegistered(accountId, WORKLOAD_IAM_SYNC);
    }

    public boolean isUserSyncThreadTimeoutEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FMS_USERSYNC_THREAD_TIMEOUT);
    }

    public boolean isFmsDelayedStopStartEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FMS_DELAYED_STOP_START);
    }

    public boolean isSaltUserPasswordRotationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ROTATE_SALTUSER_PASSWORD);
    }

    public boolean isUserSyncEnforceGroupMembershipLimitEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT);
    }

    public boolean isExperimentalNodeCountLimitsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_EXPERIMENTAL_SCALE_LIMITS);
    }

    public boolean isEmbeddedPostgresUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_POSTGRES_UPGRADE_EMBEDDED);
    }

    public boolean isPostgresUpgradeExceptionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_POSTGRES_UPGRADE_EXCEPTION);
    }

    public boolean isPostgresUpgradeSkipServicesAndCmStopEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP);
    }

    public boolean isUserSyncSplitFreeIPAUserRetrievalEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL);
    }

    public boolean isLongTimeBackupEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_BACKUP_LONG_TIMEOUT);
    }

    public boolean isPostgresUpgradeAttachedDatahubsCheckSkipped(String accountId) {
        return isEntitlementRegistered(accountId, CDP_POSTGRES_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK);
    }

    public boolean isUpgradeAttachedDatahubsCheckSkipped(String accountId) {
        return isEntitlementRegistered(accountId, CDP_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK);
    }

    public boolean isECDSABasedAccessKeyEnabled(String accountId) {
        return isEntitlementRegistered(accountId, ACCESS_KEY_ECDSA);
    }

    public boolean isEditProxyConfigEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ENVIRONMENT_EDIT_PROXY_CONFIG);
    }

    public boolean isSDXOptimizedConfigurationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, SDX_CONFIGURATION_OPTIMIZATION);
    }

    public boolean isTargetingSubnetsForEndpointAccessGatewayEnabled(String accountId) {
        return isEntitlementRegistered(accountId, TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY);
    }

    public boolean isUsdCostCalculationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_COST_CALCULATION);
    }

    public boolean isCO2CalculationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_CO2_CALCULATION);
    }

    public boolean isWiamUsersyncRoutingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, WORKLOAD_IAM_USERSYNC_ROUTING);
    }

    public List<String> getEntitlements(String accountId) {
        Account accountDetails = umsClient.getAccountDetails(
                accountId,
                regionAwareInternalCrnGeneratorFactory);
        return accountDetails.getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .collect(Collectors.toList());
    }

    private boolean isEntitlementRegistered(String accountId, Entitlement entitlement) {
        boolean entitled = getEntitlements(accountId)
                .stream()
                .map(String::toUpperCase)
                .anyMatch(e -> e.equalsIgnoreCase(entitlement.name()));
        LOGGER.debug("Entitlement result {}={}", entitlement, entitled);
        return entitled;
    }
}
