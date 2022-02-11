package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DetailedStackStatus {
    UNKNOWN(Status.UNKNOWN, AvailabilityStatus.UNKNOWN),
    // Provision statuses
    PROVISION_REQUESTED(Status.REQUESTED, AvailabilityStatus.UNAVAILABLE),
    PROVISION_SETUP(Status.CREATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    IMAGE_SETUP(Status.CREATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    CREATING_INFRASTRUCTURE(Status.CREATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    METADATA_COLLECTION(Status.CREATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    TLS_SETUP(Status.CREATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    REGISTERING_WITH_CLUSTER_PROXY(Status.CREATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    STACK_PROVISIONED(Status.STACK_AVAILABLE, AvailabilityStatus.UNAVAILABLE),
    PROVISIONED(Status.AVAILABLE, AvailabilityStatus.AVAILABLE),
    PROVISION_FAILED(Status.CREATE_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Orchestration statuses
    BOOTSTRAPPING_MACHINES(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    COLLECTING_HOST_METADATA(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    MOUNTING_DISKS(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    CONFIGURING_ORCHESTRATOR(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    VALIDATING_CLOUD_STORAGE(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    STARTING_FREEIPA_SERVICES(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    REGISTER_WITH_CLUSTER_PROXY(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    UPDATE_CLUSTER_PROXY_REGISTRATION(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    POSTINSTALL_FREEIPA_CONFIGURATION(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    // Start statuses
    START_REQUESTED(Status.START_REQUESTED, AvailabilityStatus.UNAVAILABLE),
    START_IN_PROGRESS(Status.START_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    STARTED(Status.AVAILABLE, AvailabilityStatus.AVAILABLE),
    START_FAILED(Status.START_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Stop statuses
    STOP_REQUESTED(Status.STOP_REQUESTED, AvailabilityStatus.UNAVAILABLE),
    STOP_IN_PROGRESS(Status.STOP_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    STOPPED(Status.STOPPED, AvailabilityStatus.UNAVAILABLE),
    STOP_FAILED(Status.STOP_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Upscale statuses
    UPSCALE_REQUESTED(Status.UPDATE_REQUESTED, AvailabilityStatus.AVAILABLE),
    UPSCALE_IN_PROGRESS(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.AVAILABLE),
    UPSCALE_COMPLETED(Status.AVAILABLE, AvailabilityStatus.AVAILABLE),
    UPSCALE_FAILED(Status.UPSCALE_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Downscale statuses
    DOWNSCALE_REQUESTED(Status.UPDATE_REQUESTED, AvailabilityStatus.AVAILABLE),
    DOWNSCALE_IN_PROGRESS(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.AVAILABLE),
    DOWNSCALE_COMPLETED(Status.AVAILABLE, AvailabilityStatus.AVAILABLE),
    DOWNSCALE_FAILED(Status.DOWNSCALE_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Repair statuses
    REPAIR_REQUESTED(Status.UPDATE_REQUESTED, AvailabilityStatus.AVAILABLE),
    REPAIR_IN_PROGRESS(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.AVAILABLE),
    REPAIR_COMPLETED(Status.AVAILABLE, AvailabilityStatus.AVAILABLE),
    REPAIR_FAILED(Status.REPAIR_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Termination statuses
    DEREGISTERING_WITH_CLUSTERPROXY(Status.DELETE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    DEREGISTERING_CCM_KEY(Status.DELETE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    DELETE_IN_PROGRESS(Status.DELETE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    DELETE_COMPLETED(Status.DELETE_COMPLETED, AvailabilityStatus.UNAVAILABLE),
    DELETE_FAILED(Status.DELETE_FAILED, AvailabilityStatus.UNAVAILABLE),
    // Rollback statuses
    ROLLING_BACK(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    // The stack is available status
    AVAILABLE(Status.AVAILABLE, AvailabilityStatus.AVAILABLE),
    // Instance removing status
    REMOVE_INSTANCE(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.UNAVAILABLE),
    // Cluster operation is in progress status
    CLUSTER_OPERATION(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.AVAILABLE),
    // Wait for sync status
    WAIT_FOR_SYNC(Status.WAIT_FOR_SYNC, AvailabilityStatus.UNAVAILABLE),
    // Unhealthy statuses
    UNREACHABLE(Status.UNREACHABLE, AvailabilityStatus.UNAVAILABLE),
    DELETED_ON_PROVIDER_SIDE(Status.DELETED_ON_PROVIDER_SIDE, AvailabilityStatus.UNAVAILABLE),
    UNHEALTHY(Status.UNHEALTHY, AvailabilityStatus.AVAILABLE),
    // Salt state update statuses
    SALT_STATE_UPDATE_IN_PROGRESS(Status.UPDATE_IN_PROGRESS, AvailabilityStatus.AVAILABLE),
    SALT_STATE_UPDATE_FAILED(Status.UPDATE_FAILED, AvailabilityStatus.AVAILABLE),
    // CCM upgrade statuses
    UPGRADE_CCM_REQUESTED(Status.UPGRADE_CCM_REQUESTED, AvailabilityStatus.AVAILABLE),
    UPGRADE_CCM_IN_PROGRESS(Status.UPGRADE_CCM_IN_PROGRESS, AvailabilityStatus.AVAILABLE),
    UPGRADE_CCM_FAILED(Status.UPGRADE_CCM_FAILED, AvailabilityStatus.UNAVAILABLE);

    public static final Collection<DetailedStackStatus> AVAILABLE_STATUSES;

    static {
        AVAILABLE_STATUSES = Stream.of(DetailedStackStatus.values())
                .filter(s -> s.getAvailabilityStatus().isAvailable())
                .collect(Collectors.toList());
    }

    private final Status status;

    private final AvailabilityStatus availabilityStatus;

    DetailedStackStatus(Status status, AvailabilityStatus availabilityStatus) {
        this.status = status;
        this.availabilityStatus = availabilityStatus;
    }

    public Status getStatus() {
        return status;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }
}
