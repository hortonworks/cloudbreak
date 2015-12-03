package com.sequenceiq.cloudbreak.core.flow2.stack;

public enum Msg {
    STACK_INFRASTRUCTURE_BOOTSTRAP("stack.infrastructure.bootstrap"),
    STACK_INFRASTRUCTURE_METADATA_SETUP("stack.infrastructure.metadata.setup"),
    STACK_INFRASTRUCTURE_STARTING("stack.infrastructure.starting"),
    STACK_INFRASTRUCTURE_STARTED("stack.infrastructure.started"),
    STACK_BILLING_STARTED("stack.billing.started"),
    STACK_BILLING_STOPPED("stack.billing.stopped"),
    STACK_INFRASTRUCTURE_STOPPING("stack.infrastructure.stopping"),
    STACK_INFRASTRUCTURE_STOPPED("stack.infrastructure.stopped"),
    STACK_NOTIFICATION_EMAIL("stack.notification.email"),
    STACK_DELETE_IN_PROGRESS("stack.delete.in.progress"),
    STACK_DELETE_COMPLETED("stack.delete.completed"),
    STACK_FORCED_DELETE_COMPLETED("stack.forced.delete.completed"),
    STACK_ADDING_INSTANCES("stack.adding.instances"),
    STACK_REMOVING_INSTANCE("stack.removing.instance"),
    STACK_REMOVING_INSTANCE_FINISHED("stack.removing.instance.finished"),
    STACK_METADATA_EXTEND("stack.metadata.extend"),
    STACK_BOOTSTRAP_NEW_NODES("stack.bootstrap.new.nodes"),
    STACK_UPSCALE_FINISHED("stack.upscale.finished"),
    STACK_DOWNSCALE_INSTANCES("stack.downscale.instances"),
    STACK_DOWNSCALE_SUCCESS("stack.downscale.success"),
    STACK_STOP_REQUESTED("stack.stop.requested"),
    STACK_PROVISIONING("stack.provisioning"),
    STACK_INFRASTRUCTURE_TIME("stack.infrastructure.time"),
    STACK_INFRASTRUCTURE_SUBNETS_UPDATING("stack.infrastructure.subnets.updating"),
    STACK_INFRASTRUCTURE_SUBNETS_UPDATED("stack.infrastructure.subnets.updated"),
    STACK_INFRASTRUCTURE_UPDATE_FAILED("stack.infrastructure.update.failed"),
    STACK_INFRASTRUCTURE_CREATE_FAILED("stack.infrastructure.create.failed"),
    STACK_INFRASTRUCTURE_ROLLBACK_FAILED("stack.infrastructure.rollback.failed"),
    STACK_INFRASTRUCTURE_DELETE_FAILED("stack.infrastructure.delete.failed"),
    STACK_INFRASTRUCTURE_START_FAILED("stack.infrastructure.start.failed"),
    STACK_INFRASTRUCTURE_STOP_FAILED("stack.infrastructure.stop.failed"),
    STACK_INFRASTRUCTURE_ROLLBACK_MESSAGE("stack.infrastructure.create.rollback");

    private String code;

    Msg(String msgCode) {
        code = msgCode;
    }

    public String code() {
        return code;
    }
}
