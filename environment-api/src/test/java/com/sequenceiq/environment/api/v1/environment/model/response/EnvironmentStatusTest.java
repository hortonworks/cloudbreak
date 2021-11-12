package com.sequenceiq.environment.api.v1.environment.model.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class EnvironmentStatusTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] isAvailableDataProvider() {
        return new Object[][] {
                // testCaseName                             underTest                                               availableExpected
                { "CREATION_INITIATED",                     EnvironmentStatus.CREATION_INITIATED,                   false },
                { "DELETE_INITIATED",                       EnvironmentStatus.DELETE_INITIATED,                     false },
                { "UPDATE_INITIATED",                       EnvironmentStatus.UPDATE_INITIATED,                     false },
                { "NETWORK_CREATION_IN_PROGRESS",           EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS,         false },
                { "NETWORK_DELETE_IN_PROGRESS",             EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS,           false },
                { "RDBMS_DELETE_IN_PROGRESS",               EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS,             false },
                { "FREEIPA_CREATION_IN_PROGRESS",           EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS,         false },
                { "FREEIPA_DELETE_IN_PROGRESS",             EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS,           false },
                { "IDBROKER_MAPPINGS_DELETE_IN_PROGRESS",   EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS, false },
                { "S3GUARD_TABLE_DELETE_IN_PROGRESS",       EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS,     false },
                { "DATAHUB_CLUSTERS_DELETE_IN_PROGRESS",    EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS,  false },
                { "DATALAKE_CLUSTERS_DELETE_IN_PROGRESS",   EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS, false },
                { "PUBLICKEY_CREATE_IN_PROGRESS",           EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS,         false },
                { "PUBLICKEY_DELETE_IN_PROGRESS",           EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS,         false },
                { "CLUSTER_DEFINITION_DELETE_PROGRESS",     EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS,  false },
                { "AVAILABLE",                              EnvironmentStatus.AVAILABLE,                            true },
                { "ARCHIVED",                               EnvironmentStatus.ARCHIVED,                             false },
                { "CREATE_FAILED",                          EnvironmentStatus.CREATE_FAILED,                        false },
                { "DELETE_FAILED",                          EnvironmentStatus.DELETE_FAILED,                        false },
                { "UPDATE_FAILED",                          EnvironmentStatus.UPDATE_FAILED,                        false },
                { "ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS", EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS, false },
                { "ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS", EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS, false },
                { "UPGRADE_CCM_VALIDATION_IN_PROGRESS",     EnvironmentStatus.UPGRADE_CCM_VALIDATION_IN_PROGRESS,   false },
                { "UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS",     EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS,   false },
                { "UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS",    EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS,  false },
                { "UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS",     EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS,   false },
                { "UPGRADE_CCM_ROLLING_BACK",               EnvironmentStatus.UPGRADE_CCM_ROLLING_BACK,             false },
                { "UPGRADE_CCM_VALIDATION_FAILED",          EnvironmentStatus.UPGRADE_CCM_VALIDATION_FAILED,        false },
                { "UPGRADE_CCM_ON_FREEIPA_FAILED",          EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_FAILED,        false },
                { "UPGRADE_CCM_ON_DATALAKE_FAILED",         EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_FAILED,       false },
                { "UPGRADE_CCM_ON_DATAHUB_FAILED",          EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_FAILED,        false },
                { "UPGRADE_CCM_FAILED",                     EnvironmentStatus.UPGRADE_CCM_FAILED,                   false },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("isAvailableDataProvider")
    void isAvailableTest(String testCaseName, EnvironmentStatus underTest, boolean availableExpected) {
        assertThat(underTest.isAvailable()).isEqualTo(availableExpected);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] isFailedDataProvider() {
        return new Object[][] {
                // testCaseName                             underTest                                               failedExpected
                { "CREATION_INITIATED",                     EnvironmentStatus.CREATION_INITIATED,                   false },
                { "DELETE_INITIATED",                       EnvironmentStatus.DELETE_INITIATED,                     false },
                { "UPDATE_INITIATED",                       EnvironmentStatus.UPDATE_INITIATED,                     false },
                { "NETWORK_CREATION_IN_PROGRESS",           EnvironmentStatus.NETWORK_CREATION_IN_PROGRESS,         false },
                { "NETWORK_DELETE_IN_PROGRESS",             EnvironmentStatus.NETWORK_DELETE_IN_PROGRESS,           false },
                { "FREEIPA_CREATION_IN_PROGRESS",           EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS,         false },
                { "FREEIPA_DELETE_IN_PROGRESS",             EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS,           false },
                { "RDBMS_DELETE_IN_PROGRESS",               EnvironmentStatus.RDBMS_DELETE_IN_PROGRESS,             false },
                { "IDBROKER_MAPPINGS_DELETE_IN_PROGRESS",   EnvironmentStatus.IDBROKER_MAPPINGS_DELETE_IN_PROGRESS, false },
                { "S3GUARD_TABLE_DELETE_IN_PROGRESS",       EnvironmentStatus.S3GUARD_TABLE_DELETE_IN_PROGRESS,     false },
                { "DATAHUB_CLUSTERS_DELETE_IN_PROGRESS",    EnvironmentStatus.DATAHUB_CLUSTERS_DELETE_IN_PROGRESS,  false },
                { "DATALAKE_CLUSTERS_DELETE_IN_PROGRESS",   EnvironmentStatus.DATALAKE_CLUSTERS_DELETE_IN_PROGRESS, false },
                { "PUBLICKEY_CREATE_IN_PROGRESS",           EnvironmentStatus.PUBLICKEY_CREATE_IN_PROGRESS,         false },
                { "PUBLICKEY_DELETE_IN_PROGRESS",           EnvironmentStatus.PUBLICKEY_DELETE_IN_PROGRESS,         false },
                { "CLUSTER_DEFINITION_DELETE_PROGRESS",     EnvironmentStatus.CLUSTER_DEFINITION_CLEANUP_PROGRESS,  false },
                { "AVAILABLE",                              EnvironmentStatus.AVAILABLE,                            false },
                { "ARCHIVED",                               EnvironmentStatus.ARCHIVED,                             false },
                { "CREATE_FAILED",                          EnvironmentStatus.CREATE_FAILED,                        true },
                { "DELETE_FAILED",                          EnvironmentStatus.DELETE_FAILED,                        true },
                { "UPDATE_FAILED",                          EnvironmentStatus.UPDATE_FAILED,                        true },
                { "ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS", EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_INITIALIZATION_IN_PROGRESS, false },
                { "ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS", EnvironmentStatus.ENVIRONMENT_RESOURCE_ENCRYPTION_DELETE_IN_PROGRESS, false },
                { "FREEIPA_DELETED_ON_PROVIDER_SIDE",       EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE,     true },
                { "UPGRADE_CCM_VALIDATION_IN_PROGRESS",     EnvironmentStatus.UPGRADE_CCM_VALIDATION_IN_PROGRESS,   false },
                { "UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS",     EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS,   false },
                { "UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS",    EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_IN_PROGRESS,  false },
                { "UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS",     EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_IN_PROGRESS,   false },
                { "UPGRADE_CCM_ROLLING_BACK",               EnvironmentStatus.UPGRADE_CCM_ROLLING_BACK,             false },
                { "UPGRADE_CCM_VALIDATION_FAILED",          EnvironmentStatus.UPGRADE_CCM_VALIDATION_FAILED,        true },
                { "UPGRADE_CCM_ON_FREEIPA_FAILED",          EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_FAILED,        true },
                { "UPGRADE_CCM_ON_DATALAKE_FAILED",         EnvironmentStatus.UPGRADE_CCM_ON_DATALAKE_FAILED,       true },
                { "UPGRADE_CCM_ON_DATAHUB_FAILED",          EnvironmentStatus.UPGRADE_CCM_ON_DATAHUB_FAILED,        true },
                { "UPGRADE_CCM_FAILED",                     EnvironmentStatus.UPGRADE_CCM_FAILED,                   true },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("isFailedDataProvider")
    void isFailedTest(String testCaseName, EnvironmentStatus underTest, boolean failedExpected) {
        assertThat(underTest.isFailed()).isEqualTo(failedExpected);
    }

}
