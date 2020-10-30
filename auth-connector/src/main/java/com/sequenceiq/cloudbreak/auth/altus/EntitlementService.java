package com.sequenceiq.cloudbreak.auth.altus;

import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CB_AUTHZ_POWER_USERS;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AUTOMATIC_USERSYNC_POLLER;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_BASE_IMAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_DATABASE_WIRE_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CB_FAST_EBS_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_IDENTITY_MAPPING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_CLOUD_STORAGE_VALIDATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_EMBEDDED_DATABASE_ON_ATTACHED_DISK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_ENABLE_DISTROX_INSTANCE_TYPES;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FMS_CLUSTER_PROXY;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_DL_EBS_ENCRYPTION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_HA_REPAIR;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_FREEIPA_HEALTH_CHECK;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_GCP;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_LIST_FILTERING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_MEDIUM_DUTY_SDX;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RAZ;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_RUNTIME_UPGRADE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_SDX_HBASE_CLOUD_STORAGE;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CDP_UMS_USER_SYNC_MODEL_GENERATION;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.CLOUDERA_INTERNAL_ACCOUNT;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AWS_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.DATAHUB_AZURE_AUTOSCALING;
import static com.sequenceiq.cloudbreak.auth.altus.model.Entitlement.LOCAL_DEV;

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

    public boolean isEntitledFor(String actorCrn, String accountId, Entitlement entitledFor) {
        return isEntitlementRegistered(actorCrn, accountId, entitledFor);
    }

    public boolean listFilteringEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_LIST_FILTERING);
    }

    public boolean azureEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_AZURE);
    }

    public boolean gcpEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_GCP);
    }

    public boolean baseImageEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_BASE_IMAGE);
    }

    public boolean automaticUsersyncPollerEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_AUTOMATIC_USERSYNC_POLLER);
    }

    public boolean enableDistroxInstanceTypesEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_ENABLE_DISTROX_INSTANCE_TYPES);
    }

    public boolean freeIpaHaRepairEnabled(String actorCrn, String accountID) {
        return isEntitlementRegistered(actorCrn, accountID, CDP_FREEIPA_HA_REPAIR);
    }

    public boolean freeIpaHealthCheckEnabled(String actorCrn, String accountID) {
        return isEntitlementRegistered(actorCrn, accountID, CDP_FREEIPA_HEALTH_CHECK);
    }

    public boolean internalTenant(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CLOUDERA_INTERNAL_ACCOUNT);
    }

    public boolean localDevelopment(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, LOCAL_DEV);
    }

    public boolean fmsClusterProxyEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_FMS_CLUSTER_PROXY);
    }

    public boolean cloudStorageValidationEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_CLOUD_STORAGE_VALIDATION);
    }

    public boolean razEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_RAZ);
    }

    public boolean mediumDutySdxEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_MEDIUM_DUTY_SDX);
    }

    public boolean runtimeUpgradeEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_RUNTIME_UPGRADE);
    }

    public boolean datahubRuntimeUpgradeEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_RUNTIME_UPGRADE_DATAHUB);
    }

    public boolean freeIpaDlEbsEncryptionEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_FREEIPA_DL_EBS_ENCRYPTION);
    }

    public boolean azureSingleResourceGroupDeploymentEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_AZURE_SINGLE_RESOURCE_GROUP);
    }

    public boolean azureSingleResourceGroupDedicatedStorageAccountEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT);
    }

    public boolean fastEbsEncryptionEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_CB_FAST_EBS_ENCRYPTION);
    }

    public boolean cloudIdentityMappingEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_CLOUD_IDENTITY_MAPPING);
    }

    public boolean isInternalRepositoryForUpgradeAllowed(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE);
    }

    public boolean isDifferentDataHubAndDataLakeVersionAllowed(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_ALLOW_DIFFERENT_DATAHUB_VERSION_THAN_DATALAKE);
    }

    public boolean umsUserSyncModelGenerationEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_UMS_USER_SYNC_MODEL_GENERATION);
    }

    public boolean sdxHbaseCloudStorageEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_SDX_HBASE_CLOUD_STORAGE);
    }

    public boolean awsAutoScalingEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, DATAHUB_AWS_AUTOSCALING);
    }

    public boolean azureAutoScalingEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, DATAHUB_AZURE_AUTOSCALING);
    }

    public boolean isAuthorizationEntitlementRegistered(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CB_AUTHZ_POWER_USERS);
    }

    public boolean databaseWireEncryptionEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_CB_DATABASE_WIRE_ENCRYPTION);
    }

    public boolean embeddedDatabaseOnAttachedDiskEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_EMBEDDED_DATABASE_ON_ATTACHED_DISK);
    }

    public List<String> getEntitlements(String actorCrn, String accountId) {
        return getAccount(actorCrn, accountId).getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .collect(Collectors.toList());
    }

    private boolean isEntitlementRegistered(String actorCrn, String accountId, Entitlement entitlement) {
        Account accountDetails = umsClient.getAccountDetails(actorCrn, accountId, MDCUtils.getRequestId());
        boolean entitled = accountDetails
                .getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .anyMatch(e -> e.equalsIgnoreCase(entitlement.name()));
        LOGGER.debug("Entitlement result {}={}", entitlement, entitled);
        return entitled;
    }

    private Account getAccount(String actorCrn, String accountId) {
        return umsClient.getAccountDetails(actorCrn, accountId, MDCUtils.getRequestId());
    }
}
