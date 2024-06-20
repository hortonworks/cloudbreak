package com.sequenceiq.cloudbreak.view;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupType;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(@JsonSubTypes.Type(value = InstanceMetaData.class, name = "instanceMetaData"))
public interface InstanceMetadataView {

    Long getId();

    InstanceStatus getInstanceStatus();

    String getInstanceName();

    String getStatusReason();

    Long getPrivateId();

    String getPrivateIp();

    String getPublicIp();

    Integer getSshPort();

    String getInstanceId();

    Boolean getAmbariServer();

    Boolean getClusterManagerServer();

    String getDiscoveryFQDN();

    InstanceMetadataType getInstanceMetadataType();

    String getLocalityIndicator();

    Long getStartDate();

    Long getTerminationDate();

    String getSubnetId();

    String getAvailabilityZone();

    String getRackId();

    InstanceLifeCycle getLifeCycle();

    String getVariant();

    Json getImage();

    Long getInstanceGroupId();

    String getInstanceGroupName();

    InstanceGroupType getInstanceGroupType();

    String getServerCert();

    Long getUserdataSecretResourceId();

    default String getPublicIpWrapper() {
        if (getPublicIp() == null) {
            return getPrivateIp();
        }
        return getPublicIp();
    }

    default boolean isRunning() {
        InstanceStatus instanceStatus = getInstanceStatus();
        return !isTerminated() && (InstanceStatus.CREATED.equals(instanceStatus)
                || InstanceStatus.SERVICES_RUNNING.equals(instanceStatus)
                || InstanceStatus.DECOMMISSIONED.equals(instanceStatus)
                || InstanceStatus.DECOMMISSION_FAILED.equals(instanceStatus)
                || InstanceStatus.SERVICES_HEALTHY.equals(instanceStatus)
                || InstanceStatus.SERVICES_UNHEALTHY.equals(instanceStatus));
    }

    default boolean isReachable() {
        InstanceStatus instanceStatus = getInstanceStatus();
        return isReachableOrStopped()
                && !InstanceStatus.STOPPED.equals(instanceStatus);
    }

    default boolean isReachableOrStopped() {
        InstanceStatus instanceStatus = getInstanceStatus();
        return !isTerminated()
                && !isDeletedOnProvider()
                && !InstanceStatus.ZOMBIE.equals(instanceStatus)
                && !InstanceStatus.ORCHESTRATION_FAILED.equals(instanceStatus)
                && !InstanceStatus.FAILED.equals(instanceStatus);
    }

    default boolean isDeletedOnProvider() {
        InstanceStatus instanceStatus = getInstanceStatus();
        return InstanceStatus.DELETED_ON_PROVIDER_SIDE.equals(instanceStatus) || InstanceStatus.DELETED_BY_PROVIDER.equals(instanceStatus);
    }

    default boolean isTerminated() {
        return getTerminationDate() != null;
    }

    default boolean isZombie() {
        return InstanceStatus.ZOMBIE.equals(getInstanceStatus());
    }

    default boolean isHealthy() {
        InstanceStatus instanceStatus = getInstanceStatus();
        return InstanceStatus.SERVICES_HEALTHY.equals(instanceStatus) || InstanceStatus.SERVICES_RUNNING.equals(instanceStatus);
    }

    default String getShortHostname() {
        String discoveryFQDN = getDiscoveryFQDN();
        if (discoveryFQDN == null || discoveryFQDN.isEmpty()) {
            return null;
        }
        return discoveryFQDN.split("\\.")[0];
    }

    default boolean isPrimaryGateway() {
        return InstanceMetadataType.GATEWAY_PRIMARY == getInstanceMetadataType();
    }

    default boolean isCreated() {
        return InstanceStatus.CREATED.equals(getInstanceStatus());
    }

    default String getDomain() {
        String discoveryFQDN = getDiscoveryFQDN();
        if (discoveryFQDN == null || discoveryFQDN.isEmpty()) {
            return null;
        }
        return discoveryFQDN.contains(".") ? discoveryFQDN.substring(discoveryFQDN.indexOf('.') + 1) : null;
    }

    default boolean isGatewayOrPrimaryGateway() {
        InstanceMetadataType instanceMetadataType = getInstanceMetadataType();
        return InstanceMetadataType.GATEWAY == instanceMetadataType || InstanceMetadataType.GATEWAY_PRIMARY == instanceMetadataType
                || instanceMetadataType == null && InstanceGroupType.GATEWAY == getInstanceGroupType();
    }

    default String getIpWrapper(boolean preferPrivateIp) {
        return preferPrivateIp ? getPrivateIp() : getPublicIpWrapper();
    }
}
