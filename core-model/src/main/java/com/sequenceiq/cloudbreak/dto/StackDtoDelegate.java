package com.sequenceiq.cloudbreak.dto;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.OnFailureAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.ResourceUtil;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.api.type.Tunnel;

public interface StackDtoDelegate {

    StackView getStack();

    ClusterView getCluster();

    Blueprint getBlueprint();

    Workspace getWorkspace();

    Long getWorkspaceId();

    Set<Resource> getResources();

    default String getBlueprintJsonText() {
        String result;
        if (getCluster() == null) {
            result = null;
        } else if (StringUtils.isBlank(getCluster().getExtendedBlueprintText())) {
            result = getBlueprint() == null ? null : getBlueprint().getBlueprintJsonText();
        } else {
            result = getCluster().getExtendedBlueprintText();
        }
        return result;
    }

    default String getOriginalName() {
        return getStack().getOriginalName();
    }

    default Long getId() {
        return getStack().getId();
    }

    default String getCloudPlatform() {
        return getStack().getCloudPlatform();
    }

    default Integer getGatewayPort() {
        return getStack().getGatewayPort();
    }

    default String getResourceCrn() {
        return getStack().getResourceCrn();
    }

    default String getClusterManagerIp() {
        return getCluster() == null ? null : getCluster().getClusterManagerIp();
    }

    default String getName() {
        return getStack().getName();
    }

    default StackType getType() {
        return getStack().getType();
    }

    List<InstanceMetadataView> getAllAvailableInstances();

    default Status getStatus() {
        return getStack().getStatus();
    }

    default User getCreator() {
        return getStack().getCreator();
    }

    default String getRegion() {
        return getStack().getRegion();
    }

    default String getAvailabilityZone() {
        return getStack().getAvailabilityZone();
    }

    default String getDisplayName() {
        return getStack().getDisplayName();
    }

    Tenant getTenant();

    default String getEnvironmentCrn() {
        return getStack().getEnvironmentCrn();
    }

    Long getFullNodeCount();

    default List<Resource> getDiskResources() {
        return switch (getStack().getPlatformVariant()) {
            case CloudConstants.AWS, CloudConstants.AWS_NATIVE, CloudConstants.AWS_NATIVE_GOV -> getResourcesByType(ResourceType.AWS_VOLUMESET);
            case CloudConstants.GCP -> getResourcesByType(ResourceType.GCP_ATTACHED_DISKSET);
            case CloudConstants.AZURE -> ResourceUtil.getLatestResourceByInstanceId(getResourcesByType(ResourceType.AZURE_VOLUMESET));
            default -> List.of();
        };
    }

    default List<Resource> getResourcesByType(ResourceType resourceType) {
        List<Resource> resourceList = new ArrayList<>();
        for (Resource resource : getResources()) {
            if (resourceType.equals(resource.getResourceType())) {
                resourceList.add(resource);
            }
        }
        return resourceList;
    }

    default String getStackVersion() {
        return getStack().getStackVersion();
    }

    default StackAuthentication getStackAuthentication() {
        return getStack().getStackAuthentication();
    }

    Map<String, String> getParameters();

    /**
     * @deprecated please use PlatformAwareSdxConnector instead to find out related DL CRN by environmentCrn
     * or ensure to use this only in case of VM form DL deployment
     */
    @Deprecated
    default String getDatalakeCrn() {
        return getStack().getDatalakeCrn();
    }

    Network getNetwork();

    Database getDatabase();

    List<InstanceGroupDto> getInstanceGroupDtos();

    Set<String> getAvailabilityZonesByInstanceGroup(Long instanceGroupId);

    GatewayView getGateway();

    Set<ClusterComponentView> getClusterComponents();

    Orchestrator getOrchestrator();

    default boolean hasGateway() {
        return getGateway() != null;
    }

    InstanceMetadataView getPrimaryGatewayInstance();

    SecurityConfig getSecurityConfig();

    InstanceGroupDto getInstanceGroupByInstanceGroupName(String instanceGroup);

    default String getPlatformVariant() {
        return getStack().getPlatformVariant();
    }

    default FailurePolicy getFailurePolicy() {
        return getStack().getFailurePolicy();
    }

    default OnFailureAction getOnFailureActionAction() {
        return getStack().getOnFailureActionAction();
    }

    default Tunnel getTunnel() {
        return getStack().getTunnel();
    }

    default boolean isStackInDeletionPhase() {
        return DELETE_COMPLETED.equals(getStatus()) || DELETE_IN_PROGRESS.equals(getStatus());
    }

    default String getUuid() {
        return getStack().getUuid();
    }

    default String getCustomDomain() {
        return getStack().getCustomDomain();
    }

    default boolean isClusterNameAsSubdomain() {
        return getStack().isClusterNameAsSubdomain();
    }

    default boolean isHostgroupNameAsHostname() {
        return getStack().isHostgroupNameAsHostname();
    }

    default String getCustomHostname() {
        return getStack().getCustomHostname();
    }

    List<InstanceMetadataView> getAllAvailableGatewayInstances();

    List<InstanceMetadataView> getReachableGatewayInstanceMetadata();

    default List<InstanceMetadataView> getNotTerminatedInstanceMetaData() {
        return getInstanceGroupDtos().stream()
                .flatMap(ig -> ig.getInstanceMetadataViews().stream())
                .collect(Collectors.toList());
    }

    default List<InstanceMetadataView> getNotDeletedInstanceMetaData() {
        return getNotTerminatedInstanceMetaData().stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                .collect(Collectors.toList());
    }

    default DatabaseAvailabilityType getExternalDatabaseCreationType() {
        return getDatabase().getExternalDatabaseAvailabilityType();
    }

    default List<String> getKnoxSecurityGroups() {
        return getInstanceGroupDtos().stream()
                .filter(e -> GATEWAY.equals(e.getInstanceGroup().getInstanceGroupType()))
                .map(e -> e.getInstanceGroup().getSecurityGroup().getSecurityGroupIds())
                .flatMap(Set::stream)
                .collect(Collectors.toList());
    }

    default String getExternalDatabaseEngineVersion() {
        return getDatabase().getExternalDatabaseEngineVersion();
    }

    default String getTenantName() {
        return getStack().getTenantName();
    }

    List<InstanceMetadataView> getNotTerminatedAndNotZombieGatewayInstanceMetadata();

    List<InstanceGroupView> getInstanceGroupViews();

    Json getTags();

    default String getSupportedImdsVersion() {
        return getStack().getSupportedImdsVersion();
    }

    default Long getCreated() {
        return getStack().getCreated();
    }
}
