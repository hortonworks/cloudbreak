package com.sequenceiq.datalake.entity;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CERT_ROTATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CERT_ROTATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.CHANGE_IMAGE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ADD_VOLUMES_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ADD_VOLUMES_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISK_UPDATE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISK_UPDATE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ROLLING_UPGRADE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_CCM_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_CCM_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_UPGRADE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DELETE_REQUESTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.ENVIRONMENT_CREATED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.EXTERNAL_DATABASE_CREATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.PROVISIONING_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RECOVERY_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RECOVERY_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.REPAIR_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.REPAIR_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.REQUESTED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.SALT_PASSWORD_ROTATION_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.SALT_PASSWORD_ROTATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STACK_CREATION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STACK_DELETED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STACK_DELETION_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.START_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.START_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOPPED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOP_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.STOP_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.WAIT_FOR_ENVIRONMENT;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DatalakeStatusEnumTest {

    private static final Set<DatalakeStatusEnum> DELETE_IN_PROGRESS_OR_COMPLETED_SET = EnumSet.of(EXTERNAL_DATABASE_DELETION_IN_PROGRESS, STACK_DELETED,
            STACK_DELETION_IN_PROGRESS, DELETE_REQUESTED, DELETED, DELETE_FAILED);

    private static final Map<DatalakeStatusEnum, DatalakeStatusEnum> IN_PROGRESS_TO_FAILED_MAP = new EnumMap<>(
            Map.ofEntries(entry(START_IN_PROGRESS, START_FAILED), entry(STOP_IN_PROGRESS, STOP_FAILED), entry(REQUESTED, PROVISIONING_FAILED),
                    entry(WAIT_FOR_ENVIRONMENT, PROVISIONING_FAILED), entry(STACK_CREATION_IN_PROGRESS, PROVISIONING_FAILED),
                    entry(ENVIRONMENT_CREATED, PROVISIONING_FAILED), entry(EXTERNAL_DATABASE_CREATION_IN_PROGRESS, PROVISIONING_FAILED),
                    entry(STACK_DELETION_IN_PROGRESS, DELETE_FAILED), entry(EXTERNAL_DATABASE_DELETION_IN_PROGRESS, DELETE_FAILED),
                    entry(DELETE_REQUESTED, DELETE_FAILED), entry(REPAIR_IN_PROGRESS, REPAIR_FAILED), entry(CHANGE_IMAGE_IN_PROGRESS, DATALAKE_UPGRADE_FAILED),
                    entry(DATALAKE_ROLLING_UPGRADE_IN_PROGRESS, DATALAKE_UPGRADE_FAILED),
                    entry(DATALAKE_UPGRADE_IN_PROGRESS, DATALAKE_UPGRADE_FAILED), entry(CERT_ROTATION_IN_PROGRESS, CERT_ROTATION_FAILED),
                    entry(RECOVERY_IN_PROGRESS, RECOVERY_FAILED), entry(DATALAKE_UPGRADE_CCM_IN_PROGRESS, DATALAKE_UPGRADE_CCM_FAILED),
                    entry(SALT_PASSWORD_ROTATION_IN_PROGRESS, SALT_PASSWORD_ROTATION_FAILED),
                    entry(DATALAKE_SECRET_ROTATION_IN_PROGRESS, DATALAKE_SECRET_ROTATION_FAILED),
                    entry(DATALAKE_SECRET_ROTATION_ROLLBACK_IN_PROGRESS, DATALAKE_SECRET_ROTATION_ROLLBACK_FAILED),
                    entry(DATALAKE_SECRET_ROTATION_FINALIZE_IN_PROGRESS, DATALAKE_SECRET_ROTATION_FINALIZE_FAILED),
                    entry(DATALAKE_DISK_UPDATE_IN_PROGRESS, DATALAKE_DISK_UPDATE_FAILED),
                    entry(DATALAKE_ADD_VOLUMES_IN_PROGRESS, DATALAKE_ADD_VOLUMES_FAILED),
                    entry(DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_IN_PROGRESS, DATALAKE_UPDATE_PUBLIC_DNS_ENTRIES_FAILED))
    );

    private static final Set<DatalakeStatusEnum> STOP_STATE_SET = EnumSet.of(STOPPED, STOP_IN_PROGRESS);

    @ParameterizedTest(name = "datalakeStatusEnum={0}")
    @EnumSource(DatalakeStatusEnum.class)
    void isDeleteInProgressOrCompletedTest(DatalakeStatusEnum datalakeStatusEnum) {
        assertThat(datalakeStatusEnum.isDeleteInProgressOrCompleted()).isEqualTo(DELETE_IN_PROGRESS_OR_COMPLETED_SET.contains(datalakeStatusEnum));
    }

    @ParameterizedTest(name = "datalakeStatusEnum={0}")
    @EnumSource(DatalakeStatusEnum.class)
    void mapToFailedIfInProgressTest(DatalakeStatusEnum datalakeStatusEnum) {
        DatalakeStatusEnum result = datalakeStatusEnum.mapToFailedIfInProgress();

        assertThat(result).isEqualTo(IN_PROGRESS_TO_FAILED_MAP.getOrDefault(datalakeStatusEnum, datalakeStatusEnum));
    }

    @ParameterizedTest(name = "datalakeStatusEnum={0}")
    @EnumSource(DatalakeStatusEnum.class)
    void isStopStateTest(DatalakeStatusEnum datalakeStatusEnum) {
        assertThat(datalakeStatusEnum.isStopState()).isEqualTo(STOP_STATE_SET.contains(datalakeStatusEnum));
    }

    @ParameterizedTest(name = "datalakeStatusEnum={0}")
    @EnumSource(DatalakeStatusEnum.class)
    void getDefaultResourceEventTest(DatalakeStatusEnum datalakeStatusEnum) {
        assertThat(datalakeStatusEnum.getDefaultResourceEvent()).isNotNull();
    }

}