package com.sequenceiq.cloudbreak.service.notification;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.BACKUP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DATAHUB_DISK_UPDATE_RESIZE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.LOAD_BALANCER_UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.RESTORE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPGRADE_CCM_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DECOMMISSION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETED_BY_PROVIDER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.DELETING_FROM_PROVIDER_SIDE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ORCHESTRATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REMOVING_FROM_CLUSTER_MANAGER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_UNHEALTHY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.WAITING_FOR_REPAIR;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.ZOMBIE;
import static com.sequenceiq.notification.domain.NotificationType.STACK_HEALTH;
import static com.sequenceiq.notification.domain.NotificationType.STACK_PROVISIONING;
import static com.sequenceiq.notification.domain.NotificationType.STACK_REPAIR;
import static com.sequenceiq.notification.domain.NotificationType.STACK_RESIZE;
import static com.sequenceiq.notification.domain.NotificationType.STACK_START_STOP;
import static com.sequenceiq.notification.domain.NotificationType.STACK_UPGRADE;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.notification.domain.NotificationType;

@Service
public class StackNotificationTypePreparationService {

    public NotificationType notificationType(Status newStatus) {
        return stackNotificationTargets().get(newStatus);
    }

    public boolean isNotificationRequiredByStackStatus(Status status) {
        return stackNotificationTargets().keySet().contains(status);
    }

    public boolean isNotificationRequiredByInstanceStatus(InstanceStatus status) {
        return instanceNotificationTargets().contains(status);
    }

    private Map<Status, NotificationType> stackNotificationTargets() {
        Map<Status, NotificationType> result = new HashMap<>();
        result.put(DATAHUB_DISK_UPDATE_FAILED,          STACK_RESIZE);
        result.put(DATAHUB_DISK_UPDATE_RESIZE_FAILED,   STACK_RESIZE);
        result.put(UPDATE_FAILED,                       STACK_RESIZE);
        result.put(BACKUP_FAILED,                       STACK_HEALTH);
        result.put(RESTORE_FAILED,                      STACK_REPAIR);
        result.put(RECOVERY_FAILED,                     STACK_REPAIR);
        result.put(CREATE_FAILED,                       STACK_PROVISIONING);
        result.put(ENABLE_SECURITY_FAILED,              STACK_PROVISIONING);
        result.put(START_FAILED,                        STACK_START_STOP);
        result.put(STOP_FAILED,                         STACK_START_STOP);
        result.put(NODE_FAILURE,                        STACK_HEALTH);
        result.put(EXTERNAL_DATABASE_CREATION_FAILED,   STACK_PROVISIONING);
        result.put(EXTERNAL_DATABASE_START_FAILED,      STACK_START_STOP);
        result.put(EXTERNAL_DATABASE_STOP_FAILED,       STACK_START_STOP);
        result.put(EXTERNAL_DATABASE_UPGRADE_FAILED,    STACK_UPGRADE);
        result.put(LOAD_BALANCER_UPDATE_FAILED,         STACK_UPGRADE);
        result.put(UPGRADE_CCM_FAILED,                  STACK_UPGRADE);
        result.put(DELETED_ON_PROVIDER_SIDE,            STACK_HEALTH);
        return result;
    }

    public Set<InstanceStatus> instanceNotificationTargets() {
        return Set.of(
                FAILED,
                ORCHESTRATION_FAILED,
                SERVICES_UNHEALTHY,
                WAITING_FOR_REPAIR,
                DELETING_FROM_PROVIDER_SIDE,
                InstanceStatus.DELETED_ON_PROVIDER_SIDE,
                DELETED_BY_PROVIDER,
                REMOVING_FROM_CLUSTER_MANAGER,
                DECOMMISSION_FAILED,
                ZOMBIE
        );
    }

}
