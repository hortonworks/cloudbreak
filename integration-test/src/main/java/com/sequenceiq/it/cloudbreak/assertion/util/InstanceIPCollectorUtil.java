package com.sequenceiq.it.cloudbreak.assertion.util;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static com.sequenceiq.common.api.type.InstanceGroupType.GATEWAY;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

/**
 * Collects the IPs of instances. Every method selects the IP the same way: the public IP is used only if requested and available,
 * otherwise the private one. Missing instance groups and metadata are skipped, but an instance without an IP fails the collection, so that
 * no instance is left out silently. Only the {@code getAllReachable...} variants skip such instances, for diagnostic paths.
 */
public class InstanceIPCollectorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceIPCollectorUtil.class);

    private static final String NOT_AVAILABLE = "N/A";

    private InstanceIPCollectorUtil() {
    }

    public static List<String> getAllInstanceIps(FreeIpaTestDto freeIpaTestDto, boolean publicIp) {
        return getAllFreeIpaInstanceIps(freeIpaTestDto.getResponse().getInstanceGroups(), publicIp);
    }

    public static List<String> getAllInstanceIps(SdxInternalTestDto sdxInternalTestDto, boolean publicIp) {
        return getAllInstanceIps(sdxInternalTestDto.getResponse().getStackV4Response().getInstanceGroups(), publicIp);
    }

    public static List<String> getAllInstanceIps(DistroXTestDto distroXTestDto, boolean publicIp) {
        return getAllInstanceIps(distroXTestDto.getResponse().getInstanceGroups(), publicIp);
    }

    /**
     * Collects the IPs of all instances of all the given instance groups.
     */
    public static List<String> getAllInstanceIps(Collection<InstanceGroupV4Response> instanceGroups, boolean publicIp) {
        return collectIps(instanceGroups, instanceGroup -> true, instanceMetaData -> true, publicIp);
    }

    /**
     * Collects the IPs of the instances of the instance groups whose name is one of the given host group names, in the order of the names.
     */
    public static List<String> getInstanceGroupIps(List<InstanceGroupV4Response> instanceGroups, List<String> hostGroupNames, boolean publicIp) {
        return hostGroupNames.stream()
                .flatMap(hostGroupName -> collectIps(instanceGroups, instanceGroup -> hostGroupName.equals(instanceGroup.getName()),
                        instanceMetaData -> true, publicIp).stream())
                .toList();
    }

    /**
     * Collects the IPs of all instances of the gateway instance groups.
     */
    public static List<String> getGatewayIps(Collection<InstanceGroupV4Response> instanceGroups, boolean publicIp) {
        return collectIps(instanceGroups, instanceGroup -> GATEWAY == instanceGroup.getType(), instanceMetaData -> true, publicIp);
    }

    /**
     * Collects the IPs of the primary gateway instances.
     */
    public static List<String> getPrimaryGatewayIps(Collection<InstanceGroupV4Response> instanceGroups, boolean publicIp) {
        return collectIps(instanceGroups, instanceGroup -> GATEWAY == instanceGroup.getType(),
                instanceMetaData -> GATEWAY_PRIMARY == instanceMetaData.getInstanceType(), publicIp);
    }

    /**
     * Collects the IPs of all instances of all the given FreeIPA instance groups.
     */
    public static List<String> getAllFreeIpaInstanceIps(Collection<InstanceGroupResponse> instanceGroups, boolean publicIp) {
        return collectFreeIpaIps(instanceGroups, instanceGroup -> true, instanceMetaData -> true, publicIp);
    }

    /**
     * Collects the IPs of the FreeIPA instances of the given type from the MASTER instance groups.
     */
    public static List<String> getFreeIpaMasterInstanceIps(Collection<InstanceGroupResponse> instanceGroups, InstanceMetadataType instanceMetadataType,
            boolean publicIp) {
        return collectFreeIpaIps(instanceGroups, instanceGroup -> InstanceGroupType.MASTER == instanceGroup.getType(),
                instanceMetaData -> instanceMetadataType == instanceMetaData.getInstanceType(), publicIp);
    }

    /**
     * Collects the IPs of all instances of all the given instance groups, skipping (with a warning) the instances that have no IP. Meant for
     * diagnostic paths, like log collection after a failure, where some instances may never have got an IP.
     */
    public static List<String> getAllReachableInstanceIps(Collection<InstanceGroupV4Response> instanceGroups, boolean publicIp) {
        return instances(instanceGroups, instanceGroup -> true, instanceMetaData -> true)
                .map(instanceMetaData -> findIp(instanceMetaData.getInstanceId(), instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), publicIp))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Collects the IPs of all instances of all the given FreeIPA instance groups, skipping (with a warning) the instances that have no IP. Meant for
     * diagnostic paths, like log collection after a failure, where some instances may never have got an IP.
     */
    public static List<String> getAllReachableFreeIpaInstanceIps(Collection<InstanceGroupResponse> instanceGroups, boolean publicIp) {
        return freeIpaInstances(instanceGroups, instanceGroup -> true, instanceMetaData -> true)
                .map(instanceMetaData -> findIp(instanceMetaData.getInstanceId(), instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), publicIp))
                .flatMap(Optional::stream)
                .toList();
    }

    private static List<String> collectIps(Collection<InstanceGroupV4Response> instanceGroups, Predicate<InstanceGroupV4Response> instanceGroupFilter,
            Predicate<InstanceMetaDataV4Response> instanceFilter, boolean publicIp) {
        return instances(instanceGroups, instanceGroupFilter, instanceFilter)
                .map(instanceMetaData -> selectIp(instanceMetaData.getInstanceId(), instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), publicIp))
                .toList();
    }

    private static List<String> collectFreeIpaIps(Collection<InstanceGroupResponse> instanceGroups, Predicate<InstanceGroupResponse> instanceGroupFilter,
            Predicate<InstanceMetaDataResponse> instanceFilter, boolean publicIp) {
        return freeIpaInstances(instanceGroups, instanceGroupFilter, instanceFilter)
                .map(instanceMetaData -> selectIp(instanceMetaData.getInstanceId(), instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), publicIp))
                .toList();
    }

    private static Stream<InstanceMetaDataV4Response> instances(Collection<InstanceGroupV4Response> instanceGroups,
            Predicate<InstanceGroupV4Response> instanceGroupFilter, Predicate<InstanceMetaDataV4Response> instanceFilter) {
        return instanceGroups.stream()
                .filter(Objects::nonNull)
                .filter(instanceGroupFilter)
                .map(InstanceGroupV4Response::getMetadata)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(instanceFilter);
    }

    private static Stream<InstanceMetaDataResponse> freeIpaInstances(Collection<InstanceGroupResponse> instanceGroups,
            Predicate<InstanceGroupResponse> instanceGroupFilter, Predicate<InstanceMetaDataResponse> instanceFilter) {
        return instanceGroups.stream()
                .filter(Objects::nonNull)
                .filter(instanceGroupFilter)
                .map(InstanceGroupResponse::getMetaData)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(instanceFilter);
    }

    private static String selectIp(String instanceId, String privateIp, String publicIp, boolean publicIpRequested) {
        return findIp(instanceId, privateIp, publicIp, publicIpRequested)
                .orElseThrow(() -> new TestFailException(String.format("The IP of instance [%s] cannot be collected, it has no private IP%s", instanceId,
                        publicIpRequested ? " and no available public IP" : "")));
    }

    private static Optional<String> findIp(String instanceId, String privateIp, String publicIp, boolean publicIpRequested) {
        boolean usePublicIp = publicIpRequested && StringUtils.isNotEmpty(publicIp) && !NOT_AVAILABLE.equals(publicIp);
        if (!usePublicIp && StringUtils.isBlank(privateIp)) {
            LOGGER.warn("Instance [{}] has no private IP{}.", instanceId, publicIpRequested ? " and no available public IP" : "");
            return Optional.empty();
        }
        LOGGER.info("For instance [{}] Private IP [{}] and Public IP [{}] are available. {} IP will be used!",
                instanceId, privateIp, publicIp, usePublicIp ? "Public" : "Private");
        return Optional.of(usePublicIp ? publicIp : privateIp);
    }
}
