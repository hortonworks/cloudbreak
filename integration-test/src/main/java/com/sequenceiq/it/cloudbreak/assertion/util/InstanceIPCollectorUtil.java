package com.sequenceiq.it.cloudbreak.assertion.util;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class InstanceIPCollectorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceIPCollectorUtil.class);

    private InstanceIPCollectorUtil() {

    }

    public static List<String> getAllInstanceIps(FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeipaClient, boolean publicIp) {
        return freeipaClient.getDefaultClient().getFreeIpaV1Endpoint().describe(freeIpaTestDto.getEnvironmentCrn()).getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetaData().stream())
                .filter(Objects::nonNull)
                .map(instanceMetaData -> mapInstanceToIp(instanceMetaData, publicIp))
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<String> getAllInstanceIps(SdxInternalTestDto sdxInternalTestDto, SdxClient sdxClient, boolean publicIp) {
        return sdxClient.getDefaultClient().sdxEndpoint().getDetailByCrn(sdxInternalTestDto.getCrn(), Set.of()).getStackV4Response().getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .filter(Objects::nonNull)
                .map(instanceMetaData -> mapInstanceToIp(instanceMetaData, publicIp))
                .filter(Objects::nonNull)
                .toList();
    }

    public static List<String> getAllInstanceIps(DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient, boolean publicIp) {
        return cloudbreakClient.getDefaultClient().distroXV1Endpoint().getByCrn(distroXTestDto.getCrn(), Set.of()).getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .filter(Objects::nonNull)
                .map(instanceMetaData -> mapInstanceToIp(instanceMetaData, publicIp))
                .filter(Objects::nonNull)
                .toList();
    }

    public static String mapInstanceToIp(InstanceMetaDataResponse instanceMetaDataResponse, boolean publicIp) {
        LOGGER.info("The selected FreeIPA Instance Type [{}] and the available Private IP [{}] and Public IP [{}]. {} IP will be used!",
                instanceMetaDataResponse.getInstanceType(), instanceMetaDataResponse.getPrivateIp(), instanceMetaDataResponse.getPublicIp(),
                publicIp ? "Public" : "Private");
        return publicIp ? instanceMetaDataResponse.getPublicIp() : instanceMetaDataResponse.getPrivateIp();
    }

    public static String mapInstanceToIp(InstanceMetaDataV4Response instanceMetaDataV4Response, boolean publicIp) {
        LOGGER.info("The selected Instance Type [{}] and the available Private IP [{}] and Public IP [{}]. {} IP will be used!",
                instanceMetaDataV4Response.getInstanceType(), instanceMetaDataV4Response.getPrivateIp(), instanceMetaDataV4Response.getPublicIp(),
                publicIp ? "Public" : "Private");
        return publicIp ? instanceMetaDataV4Response.getPublicIp() : instanceMetaDataV4Response.getPrivateIp();
    }
}
