package com.sequenceiq.cloudbreak.auth.altus;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.logger.MDCUtils;

@Service
public class EntitlementService {

    @VisibleForTesting
    static final String CDP_AZURE = "CDP_AZURE";

    @VisibleForTesting
    static final String CDP_GCP = "CDP_GCP";

    @VisibleForTesting
    static final String CDP_BASE_IMAGE = "CDP_BASE_IMAGE";

    @VisibleForTesting
    static final String CDP_AUTOMATIC_USERSYNC_POLLER = "CDP_AUTOMATIC_USERSYNC_POLLER";

    @VisibleForTesting
    static final String CDP_FREEIPA_HA = "CDP_FREEIPA_HA";

    @VisibleForTesting
    static final String CDP_FREEIPA_HA_REPAIR = "CDP_FREEIPA_HA_REPAIR";

    @VisibleForTesting
    static final String CLOUDERA_INTERNAL_ACCOUNT = "CLOUDERA_INTERNAL_ACCOUNT";

    @VisibleForTesting
    static final String CDP_FMS_CLUSTER_PROXY = "CDP_FMS_CLUSTER_PROXY";

    @VisibleForTesting
    static final String CDP_CLOUD_STORAGE_VALIDATION = "CDP_CLOUD_STORAGE_VALIDATION";

    @VisibleForTesting
    static final String CDP_RAZ = "CDP_RAZ";

    @VisibleForTesting
    static final String CDP_MEDIUM_DUTY_SDX = "CDP_MEDIUM_DUTY_SDX";

    @VisibleForTesting
    static final String CDP_RUNTIME_UPGRADE = "CDP_RUNTIME_UPGRADE";

    @VisibleForTesting
    static final String CDP_FREEIPA_DL_EBS_ENCRYPTION = "CDP_FREEIPA_DL_EBS_ENCRYPTION";

    @VisibleForTesting
    static final String LOCAL_DEV = "LOCAL_DEV";

    @VisibleForTesting
    static final String CDP_AZURE_SINGLE_RESOURCE_GROUP = "CDP_AZURE_SINGLE_RESOURCE_GROUP";

    @VisibleForTesting
    static final String CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT = "CDP_AZURE_SINGLE_RESOURCE_GROUP_DEDICATED_STORAGE_ACCOUNT";

    @VisibleForTesting
    static final String CDP_CB_FAST_EBS_ENCRYPTION = "CDP_CB_FAST_EBS_ENCRYPTION";

    @VisibleForTesting
    static final String CDP_CLOUD_IDENTITY_MAPPING = "CDP_CLOUD_IDENTITY_MAPPING";

    @VisibleForTesting
    static final String CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE = "CDP_ALLOW_INTERNAL_REPOSITORY_FOR_UPGRADE";

    @VisibleForTesting
    static final String CDP_UMS_USER_SYNC_MODEL_GENERATION = "CDP_UMS_USER_SYNC_MODEL_GENERATION";

    @VisibleForTesting
    static final String CDP_SDX_HBASE_CLOUD_STORAGE = "CDP_SDX_HBASE_CLOUD_STORAGE";

    @Inject
    private GrpcUmsClient umsClient;

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

    public boolean freeIpaHaEnabled(String actorCrn, String accountID) {
        return isEntitlementRegistered(actorCrn, accountID, CDP_FREEIPA_HA);
    }

    public boolean freeIpaHaRepairEnabled(String actorCrn, String accountID) {
        return isEntitlementRegistered(actorCrn, accountID, CDP_FREEIPA_HA_REPAIR);
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

    public boolean umsUserSyncModelGenerationEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_UMS_USER_SYNC_MODEL_GENERATION);
    }

    public boolean sdxHbaseCloudStorageEnabled(String actorCrn, String accountId) {
        return isEntitlementRegistered(actorCrn, accountId, CDP_SDX_HBASE_CLOUD_STORAGE);
    }

    public List<String> getEntitlements(String actorCrn, String accountId) {
        return getAccount(actorCrn, accountId).getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .collect(Collectors.toList());
    }

    private UserManagementProto.Account getAccount(String actorCrn, String accountId) {
        return umsClient.getAccountDetails(actorCrn, accountId, MDCUtils.getRequestId());
    }

    private boolean isEntitlementRegistered(String actorCrn, String accountId, String entitlement) {
        return getEntitlements(actorCrn, accountId)
                .stream()
                .anyMatch(e -> e.equalsIgnoreCase(entitlement));
    }

}
