package com.sequenceiq.freeipa.converter.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.FreeIpaLoadBalancerResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.SecurityResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationToStackAuthenticationResponseConverter;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaToFreeIpaServerResponseConverter;
import com.sequenceiq.freeipa.converter.image.ImageToImageSettingsResponseConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupToInstanceGroupResponseConverter;
import com.sequenceiq.freeipa.converter.network.NetworkToNetworkResponseConverter;
import com.sequenceiq.freeipa.converter.telemetry.TelemetryConverter;
import com.sequenceiq.freeipa.converter.usersync.UserSyncStatusToUserSyncStatusResponseConverter;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.config.FreeIpaDomainUtils;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;
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

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Inject
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @Inject
    private CrossRealmTrustService crossRealmTrustService;

    public DescribeFreeIpaResponse convert(Stack stack, ImageEntity image, FreeIpa freeIpa, Optional<UserSyncStatus> userSyncStatus,
            Boolean includeAllInstances, DetailedEnvironmentResponse environmentResponse) {
        DescribeFreeIpaResponse describeFreeIpaResponse = new DescribeFreeIpaResponse();
        describeFreeIpaResponse.setName(stack.getName());
        describeFreeIpaResponse.setEnvironmentCrn(stack.getEnvironmentCrn());
        describeFreeIpaResponse.setCrn(stack.getResourceCrn());
        describeFreeIpaResponse.setCloudPlatform(stack.getCloudPlatform());
        describeFreeIpaResponse.setVariant(stack.getPlatformvariant());
        describeFreeIpaResponse.setAuthentication(authenticationResponseConverter.convert(stack.getStackAuthentication()));
        Optional.ofNullable(image).ifPresent(i -> describeFreeIpaResponse.setImage(imageSettingsResponseConverter.convert(i)));
        Optional.ofNullable(freeIpa).ifPresent(f -> describeFreeIpaResponse.setFreeIpa(freeIpaServerResponseConverter.convert(f)));
        describeFreeIpaResponse.setNetwork(networkResponseConverter.convert(stack));
        describeFreeIpaResponse.setPlacement(convertToPlacementResponse(stack));
        describeFreeIpaResponse.setTunnel(stack.getTunnel());
        describeFreeIpaResponse.setInstanceGroups(instanceGroupConverter.convert(stack.getInstanceGroups(), includeAllInstances));
        describeFreeIpaResponse.setAvailabilityStatus(stackToAvailabilityStatusConverter.convert(stack.getStackStatus()));
        describeFreeIpaResponse.setStatus(stack.getStackStatus().getStatus());
        describeFreeIpaResponse.setStatusString(stack.getStackStatus().getStatusString());
        describeFreeIpaResponse.setStatusReason(stack.getStackStatus().getStatusReason());
        describeFreeIpaResponse.setEnableMultiAz(stack.isMultiAz());
        decorateFreeIpaServerResponseWithIps(stack.getId(), describeFreeIpaResponse.getFreeIpa(), describeFreeIpaResponse.getInstanceGroups());
        decorateFreeIpaServerResponseWithLoadBalancedHost(stack, describeFreeIpaResponse.getFreeIpa(), freeIpa);
        decorateFreeIpaServerResponseWithLoadBalancerInfo(stack.getId(), describeFreeIpaResponse);
        describeFreeIpaResponse.setAppVersion(stack.getAppVersion());
        describeFreeIpaResponse.setRecipes(freeIpaRecipeService.getRecipeNamesForStack(stack.getId()));
        decorateWithCloudStorageAndTelemetry(stack, describeFreeIpaResponse);
        userSyncStatus.ifPresent(u -> describeFreeIpaResponse.setUserSyncStatus(userSyncStatusConverter.convert(u, stack.getEnvironmentCrn())));
        describeFreeIpaResponse.setSupportedImdsVersion(stack.getSupportedImdsVersion());
        describeFreeIpaResponse.setSecurity(getSecurity(stack));
        if (environmentResponse != null && EnvironmentType.isHybridFromEnvironmentTypeString(environmentResponse.getEnvironmentType())) {
            describeFreeIpaResponse.setTrust(convertTrust(stack));
        }
        return describeFreeIpaResponse;
    }

    private void decorateFreeIpaServerResponseWithLoadBalancerInfo(Long stackId, DescribeFreeIpaResponse describeFreeIpaResponse) {
        if (Objects.nonNull(describeFreeIpaResponse)) {
            Optional<LoadBalancer> loadBalancer = freeIpaLoadBalancerService.findByStackId(stackId);
            if (loadBalancer.isPresent()) {
                FreeIpaLoadBalancerResponse freeIpaLoadBalancerResponse = new FreeIpaLoadBalancerResponse();
                LoadBalancer lb = loadBalancer.get();
                freeIpaLoadBalancerResponse.setPrivateIps(lb.getIp());
                freeIpaLoadBalancerResponse.setFqdn(lb.getFqdn());
                freeIpaLoadBalancerResponse.setResourceId(lb.getResourceId());
                describeFreeIpaResponse.setLoadBalancer(freeIpaLoadBalancerResponse);
            }
        }
    }

    private SecurityResponse getSecurity(Stack stack) {
        SecurityResponse securityResponse = new SecurityResponse();
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (securityConfig != null && securityConfig.getSeLinux() != null) {
            securityResponse.setSeLinux(securityConfig.getSeLinux().name());
        } else {
            securityResponse.setSeLinux(SeLinux.PERMISSIVE.name());
        }
        return securityResponse;
    }

    private void decorateFreeIpaServerResponseWithIps(Long stackId, FreeIpaServerResponse freeIpa, List<InstanceGroupResponse> instanceGroups) {
        if (Objects.nonNull(freeIpa)) {
            Optional<LoadBalancer> loadBalancer = freeIpaLoadBalancerService.findByStackId(stackId);
            if (loadBalancer.isPresent()) {
                freeIpa.setServerIp(loadBalancer.get().getIp());
            } else {
                Set<String> privateIps = instanceGroups.stream()
                        .flatMap(instanceGroupResponse -> instanceGroupResponse.getMetaData().stream())
                        .map(InstanceMetaDataResponse::getPrivateIp)
                        .collect(Collectors.toSet());
                freeIpa.setServerIp(privateIps);
            }
        }
    }

    private void decorateWithCloudStorageAndTelemetry(Stack stack, DescribeFreeIpaResponse response) {
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

    private void decorateFreeIpaServerResponseWithLoadBalancedHost(Stack stack, FreeIpaServerResponse freeIpaServerResponse, FreeIpa freeIpa) {
        if (Objects.nonNull(freeIpaServerResponse) && balancedDnsAvailabilityChecker.isBalancedDnsAvailable(stack)) {
            freeIpaServerResponse.setFreeIpaHost(FreeIpaDomainUtils.getFreeIpaFqdn(freeIpa.getDomain()));
            freeIpaServerResponse.setFreeIpaPort(stack.getGatewayport());
        }
    }

    private PlacementResponse convertToPlacementResponse(Stack source) {
        PlacementResponse placementResponse = new PlacementResponse();
        placementResponse.setAvailabilityZone(source.getAvailabilityZone());
        placementResponse.setRegion(source.getRegion());
        return placementResponse;
    }

    private TrustResponse convertTrust(Stack stack) {
        TrustResponse trustResponse = new TrustResponse();
        Optional<CrossRealmTrust> crossRealmTrust = crossRealmTrustService.getByStackIdIfExists(stack.getId());
        if (crossRealmTrust.isPresent()) {
            CrossRealmTrust trust = crossRealmTrust.get();
            trustResponse.setTrustStatus(trust.getTrustStatus().name());
            trustResponse.setFqdn(trust.getKdcFqdn());
            trustResponse.setOperationId(trust.getOperationId());
            trustResponse.setRealm(trust.getKdcRealm());
            trustResponse.setIp(trust.getKdcIp());
        } else {
            trustResponse.setTrustStatus(TrustStatus.TRUST_SETUP_REQUIRED.name());
        }
        return trustResponse;
    }
}
