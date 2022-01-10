package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.AUDIT_ARCHIVING_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AUTOMATIC_USERSYNC_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AWS_RESTRICTED_POLICY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_DISK_ENCRYPTION_WITH_CMK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_NATIVE_FREEIPA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_DISK_SSE_WITH_CMK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_DATABASE_WIRE_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V1_TO_V2_JUMPGATE_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_JUMPGATE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_TO_V2_JUMPGATE_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CCM_V2_USE_ONE_WAY_TLS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AWS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_ON_VM;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_DISABLE_AUTO_BUNDLE_COLLECTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_HA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CONCLUSION_CHECKER_SEND_USER_EVENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_CUSTOM_CONFIGS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_METRICS_DATABUS_PROCESSING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_NODESTATUS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_RESIZE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_METRICS_DATABUS_PROCESSING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_AWS_EFS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_DISTROX_INSTANCE_TYPES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MEDIUM_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MICRO_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_NODESTATUS_ENABLE_SALT_PING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_OS_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UNBOUND_ELIMINATION;
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
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.EPHEMERAL_DISKS_FOR_TEMP_DATA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.FMS_FREEIPA_BATCH_CALL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.LOCAL_DEV;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.PERSONAL_VIEW_CB_BY_RIGHT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.UI_EDP_PROGRESS_BAR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
public class EntitlementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementService.class);

    @Inject
    private GrpcUmsClient umsClient;

    public boolean isEntitledFor(String accountId, Entitlement entitledFor) {
        return isEntitlementRegistered(accountId, entitledFor);
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

    public boolean gcpEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_GCP);
    }

    public boolean gcpAuditEnabled(String accountId) {
        return isEntitlementRegistered(accountId, AUDIT_ARCHIVING_GCP);
    }

    public boolean awsNativeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_NATIVE);
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

    public boolean datahubMetricsDatabusProcessing(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_METRICS_DATABUS_PROCESSING);
    }

    public boolean datalakeMetricsDatabusProcessing(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_METRICS_DATABUS_PROCESSING);
    }

    public boolean nodestatusSaltPingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_NODESTATUS_ENABLE_SALT_PING);
    }

    public boolean automaticUsersyncPollerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AUTOMATIC_USERSYNC_POLLER);
    }

    public boolean enableDistroxInstanceTypesEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ENABLE_DISTROX_INSTANCE_TYPES);
    }

    public boolean freeIpaHaRepairEnabled(String accountID) {
        return isEntitlementRegistered(accountID, CDP_FREEIPA_HA_REPAIR);
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

    public boolean razEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_RAZ);
    }

    public boolean mediumDutySdxEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_MEDIUM_DUTY_SDX);
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

    public boolean gcpStopStartScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_GCP_STOP_START_SCALING);
    }

    public boolean ccmV2Enabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V2);
    }

    public boolean ccmV2JumpgateEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CCM_V2_JUMPGATE);
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

    public boolean datalakeLoadBalancerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_LOAD_BALANCER);
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

    public boolean isDatalakeLightToMediumMigrationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION);
    }

    public boolean isExperienceDeletionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT);
    }

    public boolean isAzureDiskSSEWithCMKEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_DISK_SSE_WITH_CMK);
    }

    public boolean isAWSDiskEncryptionWithCMKEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_DISK_ENCRYPTION_WITH_CMK);
    }

    public boolean datahubCustomConfigsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_CUSTOM_CONFIGS);
    }

    public boolean isDiagnosticsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_VM_DIAGNOSTICS);
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

    public boolean isGcpDiskEncryptionWithCMEKEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_GCP_DISK_ENCRYPTION_WITH_CMEK);
    }

    public boolean isDatalakeMediumDutyWithProfilerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER);
    }

    public boolean isUnboundEliminationSupported(String accountId) {
        return isEntitlementRegistered(accountId, CDP_UNBOUND_ELIMINATION);
    }

    public List<String> getEntitlements(String accountId) {
        Account accountDetails = umsClient.getAccountDetails(
                accountId,
                MDCUtils.getRequestId());
        return accountDetails.getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .collect(Collectors.toList());
    }

    private boolean isEntitlementRegistered(String accountId, Entitlement entitlement) {
        boolean entitled = getEntitlements(accountId)
                .stream()
                .map(e -> e.toUpperCase())
                .anyMatch(e -> e.equalsIgnoreCase(entitlement.name()));
        LOGGER.debug("Entitlement result {}={}", entitlement, entitled);
        return entitled;
    }
}
