package com.sequenceiq.cloudbreak.view;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_STOP_FINISHED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.NODE_FAILURE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_REQUESTED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.api.type.Tunnel;

public interface StackView extends MdcContextInfoProvider {

    Long getId();

    String getResourceCrn();

    String getName();

    String getRegion();

    Integer getGatewayPort();

    Tunnel getTunnel();

    String getEnvironmentCrn();

    StackType getType();

    String getStackVersion();

    Status getStatus();

    DetailedStackStatus getDetailedStatus();

    String getStatusReason();

    String getCloudPlatform();

    Long getCreated();

    String getDatalakeCrn();

    Json getTags();

    Long getClusterId();

    String getPlatformVariant();

    String getCustomDomain();

    String getCustomHostname();

    boolean isHostgroupNameAsHostname();

    boolean isClusterNameAsSubdomain();

    String getDisplayName();

    String getDescription();

    StackAuthentication getStackAuthentication();

    Long getTerminated();

    @Deprecated(since = "2.45.0")
    String getAvailabilityZone();

    String getUuid();

    Json getInputs();

    User getCreator();

    DatabaseAvailabilityType getExternalDatabaseCreationType();

    String getExternalDatabaseEngineVersion();

    Long getWorkspaceId();

    String getWorkspaceName();

    String getTenantName();

    Long getTenantId();

    boolean isClusterProxyRegistered();

    DnsResolverType getDomainDnsResolver();

    FailurePolicy getFailurePolicy();

    OnFailureAction getOnFailureActionAction();

    String getOriginalName();

    String getMinaSshdServiceId();

    String getCcmV2AgentCrn();

    default boolean isDatalake() {
        return getType() == StackType.DATALAKE;
    }

    default ResourceType getDiskResourceType() {
        switch (getPlatformVariant()) {
            case CloudConstants.AWS:
            case CloudConstants.AWS_NATIVE:
            case CloudConstants.AWS_NATIVE_GOV:
                return ResourceType.AWS_VOLUMESET;
            case CloudConstants.GCP:
                return ResourceType.GCP_ATTACHED_DISKSET;
            case CloudConstants.AZURE:
                return ResourceType.AZURE_VOLUMESET;
            case CloudConstants.MOCK:
                return ResourceType.MOCK_VOLUME;
            default:
                return null;
        }
    }

    default boolean isStopInProgress() {
        return STOP_IN_PROGRESS.equals(getStatus()) || STOP_REQUESTED.equals(getStatus());
    }

    default boolean isExternalDatabaseStopped() {
        return EXTERNAL_DATABASE_STOP_FINISHED.equals(getStatus());
    }

    default boolean isStopped() {
        return STOPPED.equals(getStatus());
    }

    default boolean isStackInDeletionPhase() {
        return DELETE_COMPLETED.equals(getStatus()) || DELETE_IN_PROGRESS.equals(getStatus());
    }

    default boolean isStackInDeletionOrFailedPhase() {
        return isStackInDeletionPhase() || DELETE_FAILED.equals(getStatus());
    }

    default boolean isAvailable() {
        return AVAILABLE.equals(getStatus());
    }

    default boolean isAvailableWithStoppedInstances() {
        // TODO CB-15146: This may need to change depending on the final form of how we check which operations are to be allowed
        //  when there are some STOPPED instances. The entire method may be removed.
        return isAvailable();
    }

    default boolean isModificationInProgress() {
        Status status = getStatus();
        return CREATE_IN_PROGRESS.equals(status)
                || UPDATE_IN_PROGRESS.equals(status)
                || STOP_IN_PROGRESS.equals(status)
                || START_IN_PROGRESS.equals(status)
                || DELETE_IN_PROGRESS.equals(status);
    }

    default boolean isReadyForStart() {
        return STOPPED.equals(getStatus()) || START_REQUESTED.equals(getStatus()) || START_IN_PROGRESS.equals(getStatus());
    }

    default boolean isStartFailed() {
        return START_FAILED.equals(getStatus());
    }

    default boolean hasNodeFailure() {
        return NODE_FAILURE.equals(getStatus());
    }

    default boolean isDeleteInProgress() {
        return DELETE_IN_PROGRESS.equals(getStatus());
    }

    default boolean isStartInProgress() {
        return START_IN_PROGRESS.equals(getStatus()) || START_REQUESTED.equals(getStatus());
    }

    default boolean isCreateInProgress() {
        return CREATE_IN_PROGRESS.equals(getStatus());
    }

    default boolean isCreateFailed() {
        return Status.CREATE_FAILED.equals(getStatus());
    }

    default boolean isMaintenanceModeEnabled() {
        return MAINTENANCE_MODE_ENABLED.equals(getStatus());
    }

    default boolean isReadyForStop() {
        return AVAILABLE.equals(getStatus())
                || STOPPED.equals(getStatus())
                || STOP_REQUESTED.equals(getStatus())
                || STOP_IN_PROGRESS.equals(getStatus())
                || EXTERNAL_DATABASE_STOP_FINISHED.equals(getStatus())
                || NODE_FAILURE.equals(getStatus());
    }

    default boolean isStopFailed() {
        return STOP_FAILED.equals(getStatus());
    }

    @Override
    default String getResourceType() {
        return "STACK";
    }

    @Override
    default String getResourceName() {
        return getName();
    }
}
