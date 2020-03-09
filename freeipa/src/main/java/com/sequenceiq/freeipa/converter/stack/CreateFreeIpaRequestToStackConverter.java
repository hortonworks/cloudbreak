package com.sequenceiq.freeipa.converter.stack;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.User;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationRequestToStackAuthenticationConverter;
import com.sequenceiq.freeipa.converter.backup.BackupConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupRequestToInstanceGroupConverter;
import com.sequenceiq.freeipa.converter.network.NetworkRequestToNetworkConverter;
import com.sequenceiq.freeipa.converter.telemetry.TelemetryConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.AccountTagService;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class CreateFreeIpaRequestToStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateFreeIpaRequestToStackConverter.class);

    private static final String TCP_PROTOCOL = "tcp";

    @Inject
    private StackAuthenticationRequestToStackAuthenticationConverter stackAuthenticationConverter;

    @Inject
    private InstanceGroupRequestToInstanceGroupConverter instanceGroupConverter;

    @Inject
    private NetworkRequestToNetworkConverter networkConverter;

    @Inject
    private TelemetryConverter telemetryConverter;

    @Inject
    private BackupConverter backupConverter;

    @Inject
    private CrnService crnService;

    @Inject
    private CostTagging costTagging;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AccountTagService accountTagService;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.nginx.port}")
    private Integer nginxPort;

    @Value("${freeipa.ums.user.get.timeout:10}")
    private Long userGetTimeout;

    @Value("#{'${freeipa.default.gateway.cidr}'.split(',')}")
    private Set<String> defaultGatewayCidr;

    public Stack convert(CreateFreeIpaRequest source, String accountId, Future<User> userFuture, String userCrn, String cloudPlatform) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        stack.setAccountId(accountId);
        stack.setName(source.getName());
        stack.setCreated(System.currentTimeMillis());
        stack.setResourceCrn(crnService.createCrn(accountId, Crn.ResourceType.FREEIPA));
        stack.setGatewayport(source.getGatewayPort() == null ? nginxPort : source.getGatewayPort());
        stack.setStackStatus(new StackStatus(stack, "Stack provision requested.", DetailedStackStatus.PROVISION_REQUESTED));
        stack.setAvailabilityZone(Optional.ofNullable(source.getPlacement()).map(PlacementBase::getAvailabilityZone).orElse(null));
        updateCloudPlatformAndRelatedFields(source, stack, cloudPlatform);
        stack.setStackAuthentication(stackAuthenticationConverter.convert(source.getAuthentication()));
        stack.setInstanceGroups(convertInstanceGroups(source, stack, accountId));
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
            stack.setNetwork(networkConverter.convert(source.getNetwork()));
        }
        stack.setTelemetry(telemetryConverter.convert(source.getTelemetry()));
        stack.setBackup(backupConverter.convert(source.getTelemetry()));
        decorateStackWithTunnelAndCcm(stack, source);
        updateOwnerRelatedFields(source, accountId, userFuture, userCrn, stack);
        extendGatewaySecurityGroupWithDefaultGatewayCidrs(stack);
        return stack;
    }

    private void decorateStackWithTunnelAndCcm(Stack stack, CreateFreeIpaRequest source) {
        if (source.getTunnel() != null) {
            stack.setTunnel(source.getTunnel());
            stack.setUseCcm(source.getTunnel().useCcm());
        } else if (source.getUseCcm() != null) {
            stack.setUseCcm(source.getUseCcm());
            stack.setTunnel(source.getUseCcm() ? Tunnel.CCM : Tunnel.DIRECT);
        } else {
            stack.setTunnel(Tunnel.DIRECT);
            stack.setUseCcm(Boolean.FALSE);
        }
    }

    private void extendGatewaySecurityGroupWithDefaultGatewayCidrs(Stack stack) {
        Set<InstanceGroup> gateways =
                stack.getInstanceGroups().stream().filter(ig -> InstanceGroupType.MASTER == ig.getInstanceGroupType()).collect(Collectors.toSet());
        Set<String> defaultGatewayCidrs = defaultGatewayCidr.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
        if (!defaultGatewayCidrs.isEmpty() && !stack.getTunnel().useCcm()) {
            for (InstanceGroup gateway : gateways) {
                if (CollectionUtils.isEmpty(gateway.getSecurityGroup().getSecurityGroupIds())) {
                    Set<SecurityRule> rules = gateway.getSecurityGroup().getSecurityRules();
                    defaultGatewayCidrs.forEach(cloudbreakCidr -> rules.add(createSecurityRule(gateway.getSecurityGroup(), cloudbreakCidr,
                            stack.getGatewayport().toString())));
                    LOGGER.info("The control plane cidrs {} are added to the {} gateway group for the {} port.", defaultGatewayCidrs, gateway.getGroupName(),
                            stack.getGatewayport());
                }
            }
        }
    }

    private SecurityRule createSecurityRule(SecurityGroup securityGroup, String cidr, String port) {
        SecurityRule securityRule = new SecurityRule();
        securityRule.setPorts(port);
        securityRule.setProtocol(TCP_PROTOCOL);
        securityRule.setCidr(cidr);
        securityRule.setSecurityGroup(securityGroup);
        return securityRule;
    }

    private void updateOwnerRelatedFields(CreateFreeIpaRequest source, String accountId, Future<User> userFuture, String userCrn, Stack stack) {
        String owner = accountId;
        User user = User.newBuilder().setCrn(userCrn).setEmail(accountId).build();
        try {
            user = userFuture.get(userGetTimeout, TimeUnit.SECONDS);
            owner = user.getEmail();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            String errorMessage = "Couldn't fetch user from UMS: ";
            LOGGER.error(errorMessage, e);
            stack.getStackStatus().setStatusReason(errorMessage + e.getMessage());
        }
        stack.setTags(getTags(source, user, stack));
        stack.setOwner(owner);
    }

    private void updateCloudPlatformAndRelatedFields(CreateFreeIpaRequest source, Stack stack, String cloudPlatform) {
        stack.setRegion(getRegion(source, cloudPlatform));
        stack.setCloudPlatform(cloudPlatform);
        stack.setPlatformvariant(cloudPlatform);
    }

    private String getRegion(CreateFreeIpaRequest source, String cloudPlatform) {
        if (source.getPlacement() == null) {
            return null;
        }
        if (isEmpty(source.getPlacement().getRegion())) {
            Map<Platform, Region> regions = Maps.newHashMap();
            if (isNotEmpty(defaultRegions)) {
                for (String entry : defaultRegions.split(",")) {
                    String[] keyValue = entry.split(":");
                    regions.put(platform(keyValue[0]), Region.region(keyValue[1]));
                }
                Region platformRegion = regions.get(platform(cloudPlatform));
                if (platformRegion == null || isEmpty(platformRegion.value())) {
                    throw new BadRequestException(String.format("No default region specified for: %s. Region cannot be empty.", cloudPlatform));
                }
                return platformRegion.value();
            } else {
                throw new BadRequestException("No default region is specified. Region cannot be empty.");
            }
        }
        return source.getPlacement().getRegion();
    }

    private Json getTags(CreateFreeIpaRequest source, User user, Stack stack) {
        try {
            Map<String, String> userDefined = source.getTags();
            if (userDefined == null) {
                userDefined =  new HashMap<>();
            }
            // userdefined tags comming from environment service
            return new Json(new StackTags(userDefined, new HashMap<>(), getDefaultTags(user, stack)));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

    private Map<String, String> getDefaultTags(User user, Stack stack) {
        Map<String, String> result = new HashMap<>();
        try {
            boolean internalTenant = entitlementService.internalTenant(user.getCrn(), stack.getAccountId());
            Map<String, String> accountTags = accountTagService.list();
            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                    .withCreatorCrn(user.getCrn())
                    .withEnvironmentCrn(stack.getEnvironmentCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withAccountId(stack.getAccountId())
                    .withResourceCrn(stack.getResourceCrn())
                    .withIsInternalTenant(internalTenant)
                    .withUserName(user.getEmail())
                    .withAccountTags(accountTags)
                    .build();

            result.putAll(costTagging.prepareDefaultTags(request));
        } catch (Exception e) {
            LOGGER.debug("Exception during reading default tags.", e);
        }
        return result;
    }

    private Set<InstanceGroup> convertInstanceGroups(CreateFreeIpaRequest source, Stack stack, String accountId) {
        if (CollectionUtils.isEmpty(source.getInstanceGroups())) {
            throw new BadRequestException(String.format("No instancegroups are specified. Instancegroups field cannot be empty."));
        }
        Set<InstanceGroup> convertedSet = new HashSet<>();
        source.getInstanceGroups().stream()
                .map(ig -> instanceGroupConverter.convert(ig, accountId, stack.getCloudPlatform()))
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }

}
