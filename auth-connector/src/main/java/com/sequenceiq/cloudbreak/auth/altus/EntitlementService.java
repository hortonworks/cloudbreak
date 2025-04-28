package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AWS_ARM_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AWS_ARM_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AWS_RESTRICTED_POLICY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_CERTIFICATE_AUTH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE_LONG_POLLING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_DATABASE_SINGLE_SERVER_REJECT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_IMAGE_MARKETPLACE_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AWS_VARIANT_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_ADD_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_DELETE_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_MULTIAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_AZURE_RESIZE_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_CO2_CALCULATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_COST_CALCULATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_FREEIPA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_MULTIAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_GCP_SECURE_BOOT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_SECRET_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_TLS_1_3;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_USE_DEV_TELEMETRY_YUM_REPO;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_WIRE_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CENTRAL_COMPUTE_MONITORING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AWS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION_ON_VM;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CM_BULK_HOSTS_REMOVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CONCLUSION_CHECKER_SEND_USER_EVENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CONTAINER_READY_ENV;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_DATABUS_ENDPOINT_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_EXPERIMENTAL_SCALE_LIMITS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATAHUB_FORCE_OS_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_LONG_TIMEOUT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_BACKUP_ON_RESIZE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_DB_BACKUP_ENABLE_COMPRESSION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATALAKE_RESIZE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_BACKUP_RESTORE_PERMISSION_CHECKS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_DISTROX_INSTANCE_TYPES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENDPOINT_GATEWAY_SKIP_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPERIENCE_DELETION_BY_ENVIRONMENT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EXPRESS_ONBOARDING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_DATABUS_ENDPOINT_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_LOAD_BALANCER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_REBUILD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_GCP_RAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_HYBRID_CLOUD;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_LAKEHOUSE_OPTIMIZER_ENABLED;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MICRO_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_EXCEPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_POSTGRES_UPGRADE_SKIP_SERVICE_STOP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RANGER_LDAP_USERSYNC;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SAAS_SDX_INTEGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SECURITY_ENFORCING_SELINUX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SKIP_ROLLING_UPGRADE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_TARGETED_UPSCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_TRIAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UNBOUND_ELIMINATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UPGRADE_SKIP_ATTACHED_DATAHUBS_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USERSYNC_SPLIT_FREEIPA_USER_RETRIEVAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USER_SYNC_CREDENTIALS_UPDATE_OPTIMIZATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USE_CM_SYNC_COMMAND_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_USE_DATABUS_CNAME_ENDPOINT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_VM_DIAGNOSTICS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CLOUDERA_INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_GCP_STOP_START_SCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_STOP_START_SCALING_FAILURE_RECOVERY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATALAKE_HORIZONTAL_SCALE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATA_LAKE_LIGHT_TO_MEDIUM_MIGRATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.E2E_TEST_ONLY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.ENABLE_RMS_ON_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.FMS_FREEIPA_BATCH_CALL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.JUMPGATE_ENABLE_NEW_ROOT_CA;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.LOCAL_DEV;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.MEDIUM_DUTY_UPGRADE_ON_HIGHER_RUNTIME;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_DMP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_REAL_TIME_JOBS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_SAAS_PREMIUM;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OBSERVABILITY_SAAS_TRIAL;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OJDBC_TOKEN_DH;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.OJDBC_TOKEN_DH_ONE_HOUR_TOKEN;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.PERSONAL_VIEW_CB_BY_RIGHT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.TARGETING_SUBNETS_FOR_ENDPOINT_ACCESS_GATEWAY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.UI_EDP_PROGRESS_BAR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_SYNC;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.WORKLOAD_IAM_USERSYNC_ROUTING;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.Account;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Service
public class EntitlementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntitlementService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Value("${cb.datahub.arm.enabled:false}")
    private boolean dataHubArmEnabled;

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

    public boolean enforceAwsNativeForSingleAzFreeipaEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_FREEIPA);
    }

    public boolean enforceAwsNativeForSingleAzDatahubEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATAHUB);
    }

    public boolean enforceAwsNativeForSingleAzDatalakeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_ENFORCE_AWS_NATIVE_FOR_SINGLE_AZ_DATALAKE);
    }

    public boolean awsVariantMigrationEnable(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AWS_VARIANT_MIGRATION);
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

    public boolean isCdpSaasEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SAAS);
    }

    public boolean isCdpSecurityEnforcingSELinux(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SECURITY_ENFORCING_SELINUX);
    }

    public boolean isSdxSaasIntegrationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SAAS_SDX_INTEGRATION);
    }

    public boolean enableDistroxInstanceTypesEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ENABLE_DISTROX_INSTANCE_TYPES);
    }

    public boolean cdpTrialEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_TRIAL);
    }

    public boolean isFreeIpaRebuildEnabled(String accountID) {
        return isEntitlementRegistered(accountID, CDP_FREEIPA_REBUILD);
    }

    public boolean isFreeIpaLoadBalancerEnabled(String accountID) {
        return isEntitlementRegistered(accountID, CDP_FREEIPA_LOAD_BALANCER);
    }

    public boolean internalTenant(String accountId) {
        return isEntitlementRegistered(accountId, CLOUDERA_INTERNAL_ACCOUNT);
    }

    public boolean localDevelopment(String accountId) {
        return isEntitlementRegistered(accountId, LOCAL_DEV);
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

    public boolean azureMarketplaceImagesEnabled(String accountId) {
        return true;
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

    public boolean awsStopStartScalingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATAHUB_AWS_STOP_START_SCALING);
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

    public boolean datalakeLoadBalancerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATA_LAKE_LOAD_BALANCER);
    }

    public boolean azureEndpointGatewayEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_AZURE);
    }

    public boolean gcpEndpointGatewayEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_PUBLIC_ENDPOINT_ACCESS_GATEWAY_GCP);
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

    public boolean haRepairEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_ALLOW_HA_REPAIR);
    }

    public boolean conclusionCheckerSendUserEventEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CONCLUSION_CHECKER_SEND_USER_EVENT);
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

    public boolean isSkipRollingUpgradeValidationEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SKIP_ROLLING_UPGRADE_VALIDATION);
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

    public boolean isUserSyncEnforceGroupMembershipLimitEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_USERSYNC_ENFORCE_GROUP_MEMBER_LIMIT);
    }

    public boolean isExperimentalNodeCountLimitsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_EXPERIMENTAL_SCALE_LIMITS);
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

    public boolean isRazForGcpEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_GCP_RAZ);
    }

    public boolean isFedRampExternalDatabaseForceDisabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_FEDRAMP_EXTERNAL_DATABASE_FORCE_DISABLED);
    }

    public boolean isAzureDatabaseFlexibleServerUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE);
    }

    public boolean isDatalakeDatabaseBackupCompressionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATALAKE_DB_BACKUP_ENABLE_COMPRESSION);
    }

    public boolean isDatalakeHorizontalScaleEnabled(String accountId) {
        return isEntitlementRegistered(accountId, DATALAKE_HORIZONTAL_SCALE);
    }

    public boolean isAzureMultiAzEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_MULTIAZ);
    }

    public boolean isGcpMultiAzEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_GCP_MULTIAZ);
    }

    public boolean isWireEncryptionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_WIRE_ENCRYPTION);
    }

    public boolean isSecretEncryptionEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_SECRET_ENCRYPTION);
    }

    public boolean isTlsv13Enabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_TLS_1_3);
    }

    public boolean isExpressOnboardingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_EXPRESS_ONBOARDING);
    }

    public boolean isContainerReadyEnvEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CONTAINER_READY_ENV);
    }

    public boolean isObservabilitySaasPremiumEnabled(String accountId) {
        return isEntitlementRegistered(accountId, OBSERVABILITY_SAAS_PREMIUM);
    }

    public boolean isObservabilitySaasTrialEnabled(String accountId) {
        return isEntitlementRegistered(accountId, OBSERVABILITY_SAAS_TRIAL);
    }

    public boolean isObservabilityRealTimeJobsEnabled(String accountId) {
        return isEntitlementRegistered(accountId, OBSERVABILITY_REAL_TIME_JOBS);
    }

    public boolean isObservabilityDmpEnabled(String accountId) {
        return isEntitlementRegistered(accountId, OBSERVABILITY_DMP);
    }

    public boolean isDataHubArmEnabled(String accountId) {
        if (dataHubArmEnabled) {
            LOGGER.info("Data Hub arm64 is enabled by property.");
            return true;
        }
        return isEntitledFor(accountId, CDP_AWS_ARM_DATAHUB);
    }

    public boolean isDataLakeArmEnabled(String accountId) {
        return isEntitledFor(accountId, CDP_AWS_ARM_DATALAKE);
    }

    public List<String> getEntitlements(String accountId) {
        if (Crn.isCrn(accountId)) {
            throw new IllegalArgumentException("getEntitlements was called with Crn instead of an accountId: " + accountId);
        }
        Account accountDetails = umsClient.getAccountDetails(accountId);
        return accountDetails.getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private boolean isEntitlementRegistered(String accountId, Entitlement entitlement) {
        if (Crn.isCrn(accountId)) {
            throw new IllegalArgumentException("Entitlement check was called with Crn instead of an accountId: " + accountId);
        }
        boolean entitled = getEntitlements(accountId)
                .stream()
                .map(String::toUpperCase)
                .anyMatch(e -> e.equalsIgnoreCase(entitlement.name()));
        LOGGER.atLevel(entitled ? Level.TRACE : Level.DEBUG)
                .log("Entitlement result {}={}", entitlement, entitled);
        return entitled;
    }

    public boolean isRmsEnabledOnDatalake(String accountId) {
        return isEntitlementRegistered(accountId, ENABLE_RMS_ON_DATALAKE);
    }

    public boolean azureResizeDiskEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_RESIZE_DISK);
    }

    public boolean azureDeleteDiskEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_DELETE_DISK);
    }

    public boolean azureAddDiskEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_AZURE_ADD_DISK);
    }

    public boolean hybridCloudEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_HYBRID_CLOUD);
    }

    public boolean isSdxRuntimeUpgradeEnabledOnMediumDuty(String accountId) {
        return isEntitlementRegistered(accountId, MEDIUM_DUTY_UPGRADE_ON_HIGHER_RUNTIME);
    }

    public boolean cdpSkipRdsSslCertificateRollingRotationValidation(String accountId) {
        return isEntitlementRegistered(accountId, CDP_SKIP_CERTIFICATE_ROTATION_VALIDATION);
    }

    public boolean isRangerLdapUsersyncEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_RANGER_LDAP_USERSYNC);
    }

    public boolean isGcpSecureBootEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_GCP_SECURE_BOOT);
    }

    public boolean isDatahubForceOsUpgradeEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_DATAHUB_FORCE_OS_UPGRADE);
    }

    public boolean isFlexibleServerUpgradeLongPollingEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_DATABASE_FLEXIBLE_SERVER_UPGRADE_LONG_POLLING);
    }

    public boolean isJumpgateNewRootCertEnabled(String accountId) {
        return isEntitlementRegistered(accountId, JUMPGATE_ENABLE_NEW_ROOT_CA);
    }

    public boolean isSingleServerRejectEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_AZURE_DATABASE_SINGLE_SERVER_REJECT);
    }

    public boolean isDevTelemetryRepoEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_CB_USE_DEV_TELEMETRY_YUM_REPO);
    }

    public boolean isLakehouseOptimizerEnabled(String accountId) {
        return isEntitlementRegistered(accountId, CDP_LAKEHOUSE_OPTIMIZER_ENABLED);
    }

}
