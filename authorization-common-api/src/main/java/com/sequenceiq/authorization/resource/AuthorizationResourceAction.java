package com.sequenceiq.authorization.resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum AuthorizationResourceAction {

    CHANGE_CREDENTIAL("environments/changeCredential", AuthorizationResourceType.ENVIRONMENT),
    EDIT_CREDENTIAL("environments/editCredential", AuthorizationResourceType.CREDENTIAL),
    ENVIRONMENT_VERTICAL_SCALING("environments/verticalScale", AuthorizationResourceType.ENVIRONMENT),
    EDIT_ENVIRONMENT("environments/editEnvironment", AuthorizationResourceType.ENVIRONMENT),
    START_ENVIRONMENT("environments/startEnvironment", AuthorizationResourceType.ENVIRONMENT),
    STOP_ENVIRONMENT("environments/stopEnvironment", AuthorizationResourceType.ENVIRONMENT),
    DELETE_CREDENTIAL("environments/deleteCredential", AuthorizationResourceType.CREDENTIAL),
    DESCRIBE_CREDENTIAL("environments/describeCredential", AuthorizationResourceType.CREDENTIAL),
    DESCRIBE_CREDENTIAL_ON_ENVIRONMENT("environments/describeCredential", AuthorizationResourceType.ENVIRONMENT),
    DELETE_ENVIRONMENT("environments/deleteCdpEnvironment", AuthorizationResourceType.ENVIRONMENT),
    DESCRIBE_ENVIRONMENT("environments/describeEnvironment", AuthorizationResourceType.ENVIRONMENT),
    ACCESS_ENVIRONMENT("environments/accessEnvironment", AuthorizationResourceType.ENVIRONMENT),
    ADMIN_FREEIPA("environments/adminFreeIPA", AuthorizationResourceType.ENVIRONMENT),
    REPAIR_FREEIPA("environments/repairFreeIPA", AuthorizationResourceType.ENVIRONMENT),
    SCALE_FREEIPA("environments/scaleFreeIPA", AuthorizationResourceType.ENVIRONMENT),
    UPGRADE_FREEIPA("environments/upgradeFreeIPA", AuthorizationResourceType.ENVIRONMENT),
    CREATE_CREDENTIAL("environments/createCredential", AuthorizationResourceType.CREDENTIAL),
    CREATE_ENVIRONMENT("environments/createEnvironment", AuthorizationResourceType.ENVIRONMENT),
    GET_KEYTAB("environments/getKeytab", AuthorizationResourceType.ENVIRONMENT),
    CREATE_IMAGE_CATALOG("environments/createImageCatalog", AuthorizationResourceType.IMAGE_CATALOG),
    EDIT_IMAGE_CATALOG("environments/editImageCatalog", AuthorizationResourceType.IMAGE_CATALOG),
    DESCRIBE_IMAGE_CATALOG("environments/useSharedResource", AuthorizationResourceType.IMAGE_CATALOG),
    DELETE_IMAGE_CATALOG("environments/deleteImageCatalog", AuthorizationResourceType.IMAGE_CATALOG),
    DESCRIBE_CLUSTER_DEFINITION("environments/useSharedResource", AuthorizationResourceType.CLUSTER_DEFINITION),
    DELETE_CLUSTER_DEFINITION("environments/deleteClusterDefinitions", AuthorizationResourceType.CLUSTER_DEFINITION),
    CREATE_CLUSTER_DEFINITION("environments/createClusterDefinitions", AuthorizationResourceType.CLUSTER_DEFINITION),
    DESCRIBE_CLUSTER_TEMPLATE("environments/useSharedResource", AuthorizationResourceType.CLUSTER_TEMPLATE),
    DELETE_CLUSTER_TEMPLATE("datahub/deleteClusterTemplate", AuthorizationResourceType.CLUSTER_TEMPLATE),
    CREATE_CLUSTER_TEMPLATE("datahub/createClusterTemplate", AuthorizationResourceType.CLUSTER_TEMPLATE),
    GET_OPERATION_STATUS("environments/getFreeipaOperationStatus", AuthorizationResourceType.ENVIRONMENT),
    CREATE_DATALAKE("datalake/createDatalake", AuthorizationResourceType.DATALAKE),
    RESIZE_DATALAKE("datalake/resizeDatalake", AuthorizationResourceType.DATALAKE),
    DESCRIBE_DATALAKE("datalake/describeDatalake", AuthorizationResourceType.DATALAKE),
    DESCRIBE_DETAILED_DATALAKE("datalake/describeDetailedDatalake", AuthorizationResourceType.DATALAKE),
    DELETE_DATALAKE("datalake/deleteDatalake", AuthorizationResourceType.DATALAKE),
    REPAIR_DATALAKE("datalake/repairDatalake", AuthorizationResourceType.DATALAKE),
    SYNC_DATALAKE("datalake/syncDatalake", AuthorizationResourceType.DATALAKE),
    RETRY_DATALAKE_OPERATION("datalake/retryDatalakeOperation", AuthorizationResourceType.DATALAKE),
    START_DATALAKE("datalake/startDatalake", AuthorizationResourceType.DATALAKE),
    STOP_DATALAKE("datalake/stopDatalake", AuthorizationResourceType.DATALAKE),
    UPGRADE_DATALAKE("datalake/upgradeDatalake", AuthorizationResourceType.DATALAKE),
    RECOVER_DATALAKE("datalake/recoverDatalake", AuthorizationResourceType.DATALAKE),
    DATALAKE_VERTICAL_SCALING("environments/verticalScale", AuthorizationResourceType.DATALAKE),
    DATALAKE_HORIZONTAL_SCALING("datalake/horizontalScale", AuthorizationResourceType.DATALAKE),
    SYNC_COMPONENT_VERSIONS_FROM_CM_DATALAKE("datalake/syncComponentVersionsFromCm", AuthorizationResourceType.DATALAKE),
    CHANGE_IMAGE_CATALOG_DATALAKE("datalake/changeImageCatalog", AuthorizationResourceType.DATALAKE),
    ROTATE_CERT_DATALAKE("datalake/rotateAutoTlsCertDatalake", AuthorizationResourceType.DATALAKE),
    ENVIRONMENT_CREATE_DATAHUB("environments/createDatahub", AuthorizationResourceType.ENVIRONMENT),
    DESCRIBE_DATAHUB("datahub/describeDatahub", AuthorizationResourceType.DATAHUB),
    RECOVER_DATAHUB("datahub/recoverDatahub", AuthorizationResourceType.DATAHUB),
    DELETE_DATAHUB("datahub/deleteDatahub", AuthorizationResourceType.DATAHUB),
    REPAIR_DATAHUB("datahub/repairDatahub", AuthorizationResourceType.DATAHUB),
    SYNC_DATAHUB("datahub/syncDatahub", AuthorizationResourceType.DATAHUB),
    DATAHUB_VERTICAL_SCALING("environments/verticalScale", AuthorizationResourceType.DATAHUB),
    RETRY_DATAHUB_OPERATION("datahub/retryDatahubOperation", AuthorizationResourceType.DATAHUB),
    DESCRIBE_RETRYABLE_DATAHUB_OPERATION("datahub/describeRetryableDatahubOperation", AuthorizationResourceType.DATAHUB),
    START_DATAHUB("datahub/startDatahub", AuthorizationResourceType.DATAHUB),
    STOP_DATAHUB("datahub/stopDatahub", AuthorizationResourceType.DATAHUB),
    SCALE_DATAHUB("datahub/scaleDatahub", AuthorizationResourceType.DATAHUB),
    DELETE_DATAHUB_INSTANCE("datahub/deleteDatahubInstance", AuthorizationResourceType.DATAHUB),
    SET_DATAHUB_MAINTENANCE_MODE("datahub/setDatahubMaintenanceMode", AuthorizationResourceType.DATAHUB),
    ROTATE_AUTOTLS_CERT_DATAHUB("datahub/rotateAutoTlsCertDatahub", AuthorizationResourceType.DATAHUB),
    REFRESH_RECIPES_DATAHUB("datahub/refreshRecipes", AuthorizationResourceType.DATAHUB),
    REFRESH_RECIPES_DATALAKE("datalake/refreshRecipes", AuthorizationResourceType.DATALAKE),
    UPGRADE_DATAHUB("datahub/upgradeDatahub", AuthorizationResourceType.DATAHUB),
    SYNC_COMPONENT_VERSIONS_FROM_CM_DATAHUB("datahub/syncComponentVersionsFromCm", AuthorizationResourceType.DATAHUB),
    CHANGE_IMAGE_CATALOG_DATAHUB("datahub/changeImageCatalog", AuthorizationResourceType.DATAHUB),
    DESCRIBE_DATABASE("environments/describeDatabase", AuthorizationResourceType.DATABASE),
    DESCRIBE_DATABASE_SERVER("environments/describeDatabaseServer", AuthorizationResourceType.DATABASE_SERVER),
    DELETE_DATABASE("environments/deleteDatabase", AuthorizationResourceType.DATABASE),
    DELETE_DATABASE_SERVER("environments/deleteDatabaseServer", AuthorizationResourceType.DATABASE_SERVER),
    REGISTER_DATABASE("environments/registerDatabase", AuthorizationResourceType.DATABASE),
    REGISTER_DATABASE_SERVER("environments/registerDatabaseServer", AuthorizationResourceType.DATABASE_SERVER),
    CREATE_DATABASE_SERVER("environments/createDatabaseServer", AuthorizationResourceType.DATABASE_SERVER),
    CREATE_DATABASE("environments/createDatabase", AuthorizationResourceType.DATABASE_SERVER),
    START_DATABASE_SERVER("environments/startDatabaseServer", AuthorizationResourceType.DATABASE_SERVER),
    STOP_DATABASE_SERVER("environments/stopDatabaseServer", AuthorizationResourceType.DATABASE_SERVER),
    BACKUP_DATALAKE("datalake/backupDatalake", AuthorizationResourceType.DATALAKE),
    RESTORE_DATALAKE("datalake/restoreDatalake", AuthorizationResourceType.DATALAKE),
    DESCRIBE_RECIPE("environments/useSharedResource", AuthorizationResourceType.RECIPE),
    CREATE_RECIPE("environments/createRecipe", AuthorizationResourceType.RECIPE),
    DELETE_RECIPE("environments/deleteRecipe", AuthorizationResourceType.RECIPE),
    UPGRADE_CCM("environments/upgradeCcm", AuthorizationResourceType.ENVIRONMENT),
    DESCRIBE_CUSTOM_CONFIGS("datahub/describeCustomConfigs", AuthorizationResourceType.CUSTOM_CONFIGURATIONS),
    CREATE_CUSTOM_CONFIGS("datahub/createCustomConfigs", AuthorizationResourceType.CUSTOM_CONFIGURATIONS),
    DELETE_CUSTOM_CONFIGS("datahub/deleteCustomConfigs", AuthorizationResourceType.CUSTOM_CONFIGURATIONS),
    CREATE_AUDIT_CREDENTIAL("environments/createAuditCredential", AuthorizationResourceType.AUDIT_CREDENTIAL),
    DESCRIBE_AUDIT_CREDENTIAL("environments/describeAuditCredential", AuthorizationResourceType.AUDIT_CREDENTIAL),
    ROTATE_SALTUSER_PASSWORD_ENVIRONMENT("environments/rotateSaltuserPassword", AuthorizationResourceType.ENVIRONMENT),
    ROTATE_SALTUSER_PASSWORD_DATALAKE("environments/rotateSaltuserPassword", AuthorizationResourceType.DATALAKE),
    ROTATE_SALTUSER_PASSWORD_DATAHUB("environments/rotateSaltuserPassword", AuthorizationResourceType.DATAHUB),
    UPDATE_SALT_DATALAKE("environments/updateSalt", AuthorizationResourceType.DATALAKE),
    UPDATE_SALT_DATAHUB("environments/updateSalt", AuthorizationResourceType.DATAHUB),
    MODIFY_AUDIT_CREDENTIAL("environments/modifyAuditCredential", AuthorizationResourceType.AUDIT_CREDENTIAL),
    POWERUSER_ONLY("cloudbreak/allowPowerUserOnly", null),
    LIST_ASSIGNED_ROLES("iam/listAssignedResourceRoles", null),
    STRUCTURED_EVENTS_READ("structured_events/read", AuthorizationResourceType.STRUCTURED_EVENT),
    UPDATE_AZURE_ENCRYPTION_RESOURCES("environments/updateAzureEncryptionResources", AuthorizationResourceType.ENVIRONMENT),
    ENVIRONMENT_CHANGE_FREEIPA_IMAGE("environments/changeFreeipaImageCatalog", AuthorizationResourceType.ENVIRONMENT),
    DESCRIBE_PROXY("environments/useSharedResource", AuthorizationResourceType.PROXY),
    DELETE_PROXY("environments/deleteProxyConfig", AuthorizationResourceType.PROXY),
    CREATE_PROXY("environments/createProxyConfig", AuthorizationResourceType.PROXY),
    ROTATE_DL_SECRETS("datalake/rotateSecrets", AuthorizationResourceType.DATALAKE),
    ROTATE_DH_SECRETS("datahub/rotateSecrets", AuthorizationResourceType.DATAHUB),
    ROTATE_FREEIPA_SECRETS("environments/rotateFreeipaSecrets", AuthorizationResourceType.ENVIRONMENT),
    CREATE_ENCRYPTION_PROFILE("environments/createEncryptionProfile", AuthorizationResourceType.ENCRYPTION_PROFILE),
    DESCRIBE_ENCRYPTION_PROFILE("environments/describeEncryptionProfile", AuthorizationResourceType.ENCRYPTION_PROFILE),
    DELETE_ENCRYPTION_PROFILE("environments/deleteEncryptionProfile", AuthorizationResourceType.ENCRYPTION_PROFILE),
    MIGRATE_ZOOKEEPER_TO_KRAFT("datahub/migrateZookeeperToKRaft", AuthorizationResourceType.DATAHUB),
    FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION("datahub/finalizeZookeeperToKRaftMigration", AuthorizationResourceType.DATAHUB),
    ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION("datahub/rollbackZookeeperToKRaftMigration", AuthorizationResourceType.DATAHUB),
    // deprecated actions, please do not use them
    ENVIRONMENT_READ("environments/read", AuthorizationResourceType.ENVIRONMENT),
    ENVIRONMENT_WRITE("environments/write", AuthorizationResourceType.ENVIRONMENT),
    DATALAKE_READ("datalake/read", AuthorizationResourceType.DATALAKE),
    DATALAKE_WRITE("datalake/write", AuthorizationResourceType.DATALAKE),
    DATAHUB_READ("datahub/read", AuthorizationResourceType.DATAHUB),
    DATAHUB_WRITE("datahub/write", AuthorizationResourceType.DATAHUB);

    private static final Map<String, List<AuthorizationResourceAction>> BY_RIGHT = Stream.of(AuthorizationResourceAction.values())
            .collect(Collectors.groupingBy(AuthorizationResourceAction::getRight));

    private final String right;

    private final AuthorizationResourceType authorizationResourceType;

    AuthorizationResourceAction(String right, AuthorizationResourceType authorizationResourceType) {
        this.right = right;
        this.authorizationResourceType = authorizationResourceType;
    }

    public String getRight() {
        return right;
    }

    public AuthorizationResourceType getAuthorizationResourceType() {
        return authorizationResourceType;
    }

}
