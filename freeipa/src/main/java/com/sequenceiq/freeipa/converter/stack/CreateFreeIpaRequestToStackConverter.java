package com.sequenceiq.freeipa.converter.stack;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.region.PlacementBase;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationRequestToStackAuthenticationConverter;
import com.sequenceiq.freeipa.converter.credential.CredentialRequestToCredentialConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupRequestToInstanceGroupConverter;
import com.sequenceiq.freeipa.converter.network.NetworkRequestToNetworkConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.CostTaggingService;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;

@Component
public class CreateFreeIpaRequestToStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateFreeIpaRequestToStackConverter.class);

    @Inject
    private CostTaggingService costTaggingService;

    @Inject
    private StackAuthenticationRequestToStackAuthenticationConverter stackAuthenticationConverter;

    @Inject
    private InstanceGroupRequestToInstanceGroupConverter instanceGroupConverter;

    @Inject
    private NetworkRequestToNetworkConverter networkConverter;

    @Inject
    private CredentialRequestToCredentialConverter credentialConverter;

    @Inject
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.nginx.port:9443}")
    private Integer nginxPort;

    public Stack convert(CreateFreeIpaRequest source, String accountId, String userId) {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(source.getEnvironmentCrn());
        stack.setAccountId(accountId);
        stack.setName(source.getName());
        stack.setCreated(System.currentTimeMillis());
        stack.setGatewayport(nginxPort);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setAvailabilityZone(Optional.ofNullable(source.getPlacement()).map(PlacementBase::getAvailabilityZone).orElse(null));
        updateCloudPlatformAndRelatedFields(source, stack, accountId);
        stack.setStackAuthentication(stackAuthenticationConverter.convert(source.getAuthentication()));
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(CloudPlatform.valueOf(source.getCredential().getCloudPlatform()));
            stack.setNetwork(networkConverter.convert(source.getNetwork()));
        }
        stack.setCredential(credentialConverter.convert(source.getCredential()));
        stack.setOwner(Optional.of(userId).orElse(accountId));
        return stack;
    }

    private void updateCloudPlatformAndRelatedFields(CreateFreeIpaRequest source, Stack stack, String owner) {
        String cloudPlatform = source.getCredential().getCloudPlatform();
        stack.setRegion(getRegion(source, cloudPlatform));
        stack.setCloudPlatform(cloudPlatform);
        stack.setTags(getTags(owner, cloudPlatform));
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

    private Json getTags(String owner, String cloudPlatform) {
        try {
            return new Json(new StackTags(new HashMap<>(), new HashMap<>(), getDefaultTags(owner, cloudPlatform)));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

    private Map<String, String> getDefaultTags(String owner, String cloudPlatform) {
        Map<String, String> result = new HashMap<>();
        try {
            result.putAll(costTaggingService.prepareDefaultTags(owner, result, cloudPlatform));
        } catch (Exception e) {
            LOGGER.debug("Exception during reading default tags.", e);
        }
        return result;
    }

    private Set<InstanceGroup> convertInstanceGroups(CreateFreeIpaRequest source, Stack stack) {
        if (source.getInstanceGroups() == null) {
            Set<InstanceGroup> defaultInstanceGroups = defaultInstanceGroupProvider.createDefaultInstanceGroups(source.getCredential().getCloudPlatform());
            defaultInstanceGroups.forEach(instanceGroup -> instanceGroup.setStack(stack));
            return defaultInstanceGroups;
        }
        Set<InstanceGroup> convertedSet = new HashSet<>();
        source.getInstanceGroups().stream()
                .map(ig -> instanceGroupConverter.convert(ig, source.getCredential().getCloudPlatform()))
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }
}
