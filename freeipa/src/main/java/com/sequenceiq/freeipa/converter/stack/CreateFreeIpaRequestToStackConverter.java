package com.sequenceiq.freeipa.converter.stack;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
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
import com.sequenceiq.freeipa.service.tag.AccountTagService;
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

    public Stack convert(CreateFreeIpaRequest source, String accountId, Future<String> ownerFuture, String userCrn, String cloudPlatform) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        stack.setAccountId(accountId);
        stack.setName(source.getName());
        stack.setCreated(System.currentTimeMillis());
        stack.setResourceCrn(crnService.createCrn(accountId, CrnResourceDescriptor.FREEIPA));
        MDCBuilder.addResourceCrn(stack.getResourceCrn());
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
        if (source.getBackup() != null && isNotEmpty(source.getBackup().getStorageLocation())) {
            stack.setBackup(backupConverter.convert(source.getBackup()));
        } else {
            stack.setBackup(backupConverter.convert(source.getTelemetry()));
        }
        stack.setCdpNodeStatusMonitorUser(UUID.randomUUID().toString());
        stack.setCdpNodeStatusMonitorPassword(PasswordUtil.generatePassword());
        decorateStackWithTunnelAndCcm(stack, source);
        updateOwnerRelatedFields(source, accountId, ownerFuture, userCrn, stack);
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

    private void updateOwnerRelatedFields(CreateFreeIpaRequest source, String accountId,
            Future<String> ownerFuture, String userCrn, Stack stack) {
        String owner = accountId;
        try {
            owner = ownerFuture.get(userGetTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            String errorMessage = "Couldn't fetch user from UMS: ";
            LOGGER.error(errorMessage, e);
            stack.getStackStatus().setStatusReason(errorMessage + e.getMessage());
        }
        stack.setTags(getTags(source, stack, userCrn, owner));
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

    private Json getTags(CreateFreeIpaRequest source, Stack stack, String userCrn, String userName) {
        try {
            Map<String, String> userDefined = source.getTags();
            if (userDefined == null) {
                userDefined =  new HashMap<>();
            }
            // userdefined tags comming from environment service
            return new Json(new StackTags(userDefined, new HashMap<>(), getDefaultTags(stack, userCrn, userName)));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags: " + ignored.getMessage(), ignored);
        }
    }

    private Map<String, String> getDefaultTags(Stack stack, String userCrn, String userName) {
        Map<String, String> result = new HashMap<>();
        try {
            boolean internalTenant = entitlementService.internalTenant(stack.getAccountId());
            Map<String, String> accountTags = accountTagService.list();
            CDPTagGenerationRequest request = CDPTagGenerationRequest.Builder.builder()
                    .withCreatorCrn(userCrn)
                    .withEnvironmentCrn(stack.getEnvironmentCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withAccountId(stack.getAccountId())
                    .withResourceCrn(stack.getResourceCrn())
                    .withIsInternalTenant(internalTenant)
                    .withUserName(userName)
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
                .map(ig -> {
                    FreeIpaServerRequest ipaServerRequest = source.getFreeIpa();
                    return instanceGroupConverter.convert(ig, accountId, stack.getCloudPlatform(), stack.getName(),
                            ipaServerRequest.getHostname(), ipaServerRequest.getDomain());
                })
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }

}
