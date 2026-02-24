package com.sequenceiq.cloudbreak.service.notification;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSIONED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETING_FROM_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REMOVING_FROM_CLUSTER_MANAGER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.RESTARTING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_HEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.TERMINATED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.UNDER_DECOMMISSION;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.WAITING_FOR_REPAIR;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ZOMBIE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.notification.domain.NotificationType;

class StackNotificationTypePreparationServiceTest {

    private StackNotificationTypePreparationService underTest;

    @BeforeEach
    void setUp() {
        underTest = new StackNotificationTypePreparationService();
    }

    @Test
    void notificationType() {
        assertEquals(NotificationType.STACK_RESIZE, underTest.notificationType(Status.DATAHUB_DISK_UPDATE_FAILED));
        assertEquals(NotificationType.STACK_RESIZE, underTest.notificationType(Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED));
        assertEquals(NotificationType.STACK_RESIZE, underTest.notificationType(Status.UPDATE_FAILED));
        assertEquals(NotificationType.STACK_HEALTH, underTest.notificationType(Status.BACKUP_FAILED));
        assertEquals(NotificationType.STACK_REPAIR, underTest.notificationType(Status.RESTORE_FAILED));
        assertEquals(NotificationType.STACK_REPAIR, underTest.notificationType(Status.RECOVERY_FAILED));
        assertEquals(NotificationType.STACK_PROVISIONING, underTest.notificationType(Status.CREATE_FAILED));
        assertEquals(NotificationType.STACK_PROVISIONING, underTest.notificationType(Status.ENABLE_SECURITY_FAILED));
        assertEquals(NotificationType.STACK_START_STOP, underTest.notificationType(Status.START_FAILED));
        assertEquals(NotificationType.STACK_START_STOP, underTest.notificationType(Status.STOP_FAILED));
        assertEquals(NotificationType.STACK_HEALTH, underTest.notificationType(Status.NODE_FAILURE));
        assertEquals(NotificationType.STACK_PROVISIONING, underTest.notificationType(Status.EXTERNAL_DATABASE_CREATION_FAILED));
        assertEquals(NotificationType.STACK_START_STOP, underTest.notificationType(Status.EXTERNAL_DATABASE_START_FAILED));
        assertEquals(NotificationType.STACK_START_STOP, underTest.notificationType(Status.EXTERNAL_DATABASE_STOP_FAILED));
        assertEquals(NotificationType.STACK_UPGRADE, underTest.notificationType(Status.EXTERNAL_DATABASE_UPGRADE_FAILED));
        assertEquals(NotificationType.STACK_UPGRADE, underTest.notificationType(Status.LOAD_BALANCER_UPDATE_FAILED));
        assertEquals(NotificationType.STACK_UPGRADE, underTest.notificationType(Status.UPGRADE_CCM_FAILED));
        assertEquals(NotificationType.STACK_HEALTH, underTest.notificationType(Status.DELETED_ON_PROVIDER_SIDE));
    }

    @Test
    void isStackNotificationRequired() {
        for (Map.Entry<Status, Boolean> entry : stackStatusMap().entrySet()) {
            assertEquals(entry.getValue(), underTest.isNotificationRequiredByStackStatus(entry.getKey()));
        }
        assertEquals(stackStatusMap().entrySet().size(), Status.values().length);
    }

    @Test
    void isInstanceNotificationRequired() {
        for (Map.Entry<InstanceStatus, Boolean> entry : instanceStatusMap().entrySet()) {
            assertEquals(entry.getValue(), underTest.isNotificationRequiredByInstanceStatus(entry.getKey()));
        }
        assertEquals(instanceStatusMap().entrySet().size(), InstanceStatus.values().length);
    }

    private Map<InstanceStatus, Boolean> instanceStatusMap() {
        Map<InstanceStatus, Boolean> result = new HashMap<>();

        result.put(REQUESTED,                       false);
        result.put(CREATED,                         false);
        result.put(SERVICES_RUNNING,                false);
        result.put(SERVICES_HEALTHY,                false);
        result.put(STOPPED,                         false);
        result.put(DELETE_REQUESTED,                false);
        result.put(UNDER_DECOMMISSION,              false);
        result.put(DECOMMISSIONED,                  false);
        result.put(RESTARTING,                      false);
        result.put(TERMINATED,                      false);

        result.put(FAILED,                          true);
        result.put(ORCHESTRATION_FAILED,            true);
        result.put(SERVICES_UNHEALTHY,              true);
        result.put(WAITING_FOR_REPAIR,              true);
        result.put(DELETING_FROM_PROVIDER_SIDE,     true);
        result.put(DELETED_ON_PROVIDER_SIDE,        true);
        result.put(DELETED_BY_PROVIDER,             true);
        result.put(REMOVING_FROM_CLUSTER_MANAGER,   true);
        result.put(DECOMMISSION_FAILED,             true);
        result.put(ZOMBIE,                          true);

        return result;
    }

    private Map<Status, Boolean> stackStatusMap() {
        Map<Status, Boolean> result = new HashMap<>();

        result.put(Status.DATAHUB_DISK_UPDATE_FAILED,                   true);
        result.put(Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED,            true);
        result.put(Status.UPDATE_FAILED,                                true);
        result.put(Status.BACKUP_FAILED,                                true);
        result.put(Status.RESTORE_FAILED,                               true);
        result.put(Status.RECOVERY_FAILED,                              true);
        result.put(Status.CREATE_FAILED,                                true);
        result.put(Status.ENABLE_SECURITY_FAILED,                       true);
        result.put(Status.START_FAILED,                                 true);
        result.put(Status.STOP_FAILED,                                  true);
        result.put(Status.NODE_FAILURE,                                 true);
        result.put(Status.EXTERNAL_DATABASE_CREATION_FAILED,            true);
        result.put(Status.EXTERNAL_DATABASE_START_FAILED,               true);
        result.put(Status.EXTERNAL_DATABASE_STOP_FAILED,                true);
        result.put(Status.EXTERNAL_DATABASE_UPGRADE_FAILED,             true);
        result.put(Status.LOAD_BALANCER_UPDATE_FAILED,                  true);
        result.put(Status.UPGRADE_CCM_FAILED,                           true);
        result.put(Status.DELETED_ON_PROVIDER_SIDE,                     true);

        result.put(Status.AVAILABLE,                                    false);
        result.put(Status.REQUESTED,                                    false);
        result.put(Status.CREATE_IN_PROGRESS,                           false);
        result.put(Status.DATAHUB_DISK_UPDATE_VALIDATION_IN_PROGRESS,   false);
        result.put(Status.DATAHUB_DISK_UPDATE_VALIDATION_FAILED,        false);
        result.put(Status.UPDATE_IN_PROGRESS,                           false);
        result.put(Status.UPDATE_REQUESTED,                             false);
        result.put(Status.BACKUP_IN_PROGRESS,                           false);
        result.put(Status.BACKUP_FINISHED,                              false);
        result.put(Status.RESTORE_IN_PROGRESS,                          false);
        result.put(Status.RESTORE_FINISHED,                             false);
        result.put(Status.RECOVERY_IN_PROGRESS,                         false);
        result.put(Status.RECOVERY_REQUESTED,                           false);
        result.put(Status.PRE_DELETE_IN_PROGRESS,                       false);
        result.put(Status.DELETE_IN_PROGRESS,                           false);
        result.put(Status.DELETE_FAILED,                                false);
        result.put(Status.DELETE_COMPLETED,                             false);
        result.put(Status.STOPPED,                                      false);
        result.put(Status.STOP_REQUESTED,                               false);
        result.put(Status.START_REQUESTED,                              false);
        result.put(Status.STOP_IN_PROGRESS,                             false);
        result.put(Status.START_IN_PROGRESS,                            false);
        result.put(Status.WAIT_FOR_SYNC,                                false);
        result.put(Status.MAINTENANCE_MODE_ENABLED,                     false);
        result.put(Status.AMBIGUOUS,                                    false);
        result.put(Status.UNREACHABLE,                                  false);
        result.put(Status.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,       false);
        result.put(Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,       false);
        result.put(Status.EXTERNAL_DATABASE_DELETION_FINISHED,          false);
        result.put(Status.EXTERNAL_DATABASE_DELETION_FAILED,            false);
        result.put(Status.EXTERNAL_DATABASE_START_IN_PROGRESS,          false);
        result.put(Status.EXTERNAL_DATABASE_START_FINISHED,             false);
        result.put(Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS,           false);
        result.put(Status.EXTERNAL_DATABASE_STOP_FINISHED,              false);
        result.put(Status.EXTERNAL_DATABASE_UPGRADE_IN_PROGRESS,        false);
        result.put(Status.EXTERNAL_DATABASE_UPGRADE_FINISHED,           false);
        result.put(Status.LOAD_BALANCER_UPDATE_IN_PROGRESS,             false);
        result.put(Status.LOAD_BALANCER_UPDATE_FINISHED,                false);
        result.put(Status.UPGRADE_CCM_IN_PROGRESS,                      false);
        result.put(Status.UPGRADE_CCM_FINISHED,                         false);
        result.put(Status.DETERMINE_DATALAKE_DATA_SIZES_IN_PROGRESS,    false);
        result.put(Status.STALE,                                        false);

        return result;
    }
}
