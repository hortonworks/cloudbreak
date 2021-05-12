package com.sequenceiq.freeipa.converter.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationToStackAuthenticationResponseConverter;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaToFreeIpaServerResponseConverter;
import com.sequenceiq.freeipa.converter.image.ImageToImageSettingsResponseConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupToInstanceGroupResponseConverter;
import com.sequenceiq.freeipa.converter.network.NetworkToNetworkResponseConverter;
import com.sequenceiq.freeipa.converter.telemetry.TelemetryConverter;
import com.sequenceiq.freeipa.converter.usersync.UserSyncStatusToUserSyncStatusResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils;
import com.sequenceiq.freeipa.util.BalancedDnsAvailabilityChecker;

@Component
public class StackToDescribeFreeIpaResponseConverter {

    @Inject
    private StackAuthenticationToStackAuthenticationResponseConverter authenticationResponseConverter;

    @Inject
    private ImageToImageSettingsResponseConverter imageSettingsResponseConverter;

    @Inject
    private FreeIpaToFreeIpaServerResponseConverter freeIpaServerResponseConverter;

    @Inject
    private NetworkToNetworkResponseConverter networkResponseConverter;

    @Inject
    private InstanceGroupToInstanceGroupResponseConverter instanceGroupConverter;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private UserSyncStatusToUserSyncStatusResponseConverter userSyncStatusConverter;

    @Inject
    private BalancedDnsAvailabilityChecker balancedDnsAvailabilityChecker;

    @Inject
    private StackToAvailabilityStatusConverter stackToAvailabilityStatusConverter;

    public DescribeFreeIpaResponse convert(Stack stack, ImageEntity image, FreeIpa freeIpa, UserSyncStatus userSyncStatus) {
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setName(stack.getName());
        describeFreeIpaResponse.setEnvironmentCrn(stack.getEnvironmentCrn());
        describeFreeIpaResponse.setCrn(stack.getResourceCrn());
        describeFreeIpaResponse.setCloudPlatform(stack.getCloudPlatform());
        describeFreeIpaResponse.setAuthentication(authenticationResponseConverter.convert(stack.getStackAuthentication()));
        Optional.ofNullable(image).ifPresent(i -> describeFreeIpaResponse.setImage(imageSettingsResponseConverter.convert(i)));
        Optional.ofNullable(freeIpa).ifPresent(f -> describeFreeIpaResponse.setFreeIpa(freeIpaServerResponseConverter.convert(f)));
        describeFreeIpaResponse.setNetwork(networkResponseConverter.convert(stack));
        describeFreeIpaResponse.setPlacement(convert(stack));
        describeFreeIpaResponse.setInstanceGroups(instanceGroupConverter.convert(stack.getInstanceGroups()));
        describeFreeIpaResponse.setAvailabilityStatus(stackToAvailabilityStatusConverter.convert(stack));
        describeFreeIpaResponse.setStatus(stack.getStackStatus().getStatus());
        describeFreeIpaResponse.setStatusString(stack.getStackStatus().getStatusString());
        describeFreeIpaResponse.setStatusReason(stack.getStackStatus().getStatusReason());
        decorateFreeIpaServerResponseWithIps(describeFreeIpaResponse.getFreeIpa(), describeFreeIpaResponse.getInstanceGroups());
        decoreateFreeIpaServerResponseWithLoadBalancedHost(stack, describeFreeIpaResponse.getFreeIpa(), freeIpa);
        describeFreeIpaResponse.setAppVersion(stack.getAppVersion());
        decorateWithCloudStorgeAndTelemetry(stack, describeFreeIpaResponse);
        Optional.ofNullable(userSyncStatus).ifPresent(u -> describeFreeIpaResponse.setUserSyncStatus(userSyncStatusConverter.convert(u)));
        return describeFreeIpaResponse;
    }

    private void decorateFreeIpaServerResponseWithIps(FreeIpaServerResponse freeIpa, List<InstanceGroupResponse> instanceGroups) {
        if (Objects.nonNull(freeIpa)) {
            Set<String> privateIps = instanceGroups.stream()
                    .flatMap(instanceGroupResponse -> instanceGroupResponse.getMetaData().stream())
                    .map(InstanceMetaDataResponse::getPrivateIp)
                    .collect(Collectors.toSet());
            freeIpa.setServerIp(privateIps);
        }
    }

    private void decorateWithCloudStorgeAndTelemetry(Stack stack, DescribeFreeIpaResponse response) {
        TelemetryResponse telemetryResponse = telemetryConverter.convert(stack.getTelemetry());
        if (telemetryResponse != null) {
            response.setTelemetry(telemetryResponse);
            if (telemetryResponse.getLogging() != null) {
                CloudStorageResponse cloudStorageResponse = new CloudStorageResponse();
                List<StorageIdentityBase> identities = new ArrayList<>();
                StorageIdentityBase logIdentity = new StorageIdentityBase();
                logIdentity.setType(CloudIdentityType.LOG);
                identities.add(logIdentity);
                cloudStorageResponse.setIdentities(identities);
                response.setCloudStorage(cloudStorageResponse);
            }
        }
    }

    private void decoreateFreeIpaServerResponseWithLoadBalancedHost(Stack stack, FreeIpaServerResponse freeIpaServerResponse, FreeIpa freeIpa) {
        if (Objects.nonNull(freeIpaServerResponse) && balancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack)) {
            freeIpaServerResponse.setFreeIpaHost(FreeIpaDomainUtils.getFreeIpaFqdn(freeIpa.getDomain()));
            freeIpaServerResponse.setFreeIpaPort(stack.getGatewayport());
        }
    }

    private PlacementResponse convert(Stack source) {
        PlacementResponse placementResponse = new PlacementResponse();
        placementResponse.setAvailabilityZone(source.getAvailabilityZone());
        placementResponse.setRegion(source.getRegion());
        return placementResponse;
    }
}
