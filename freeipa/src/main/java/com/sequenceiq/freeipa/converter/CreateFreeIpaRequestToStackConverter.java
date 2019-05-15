package com.sequenceiq.freeipa.converter;

import static com.gs.collections.impl.utility.StringIterate.isEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.converter.authentication.StackAuthenticationV4RequestToStackAuthenticationConverter;
import com.sequenceiq.freeipa.converter.credential.CredentialV4RequestToCredentialConverter;
import com.sequenceiq.freeipa.converter.instance.InstanceGroupV4RequestToInstanceGroupConverter;
import com.sequenceiq.freeipa.converter.network.NetworkV4RequestToNetworkConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.service.CostTaggingService;

@Component
public class CreateFreeIpaRequestToStackConverter implements Converter<CreateFreeIpaRequest, Stack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateFreeIpaRequestToStackConverter.class);

    @Inject
    private CostTaggingService costTaggingService;

    @Inject
    private StackAuthenticationV4RequestToStackAuthenticationConverter stackAuthenticationConverter;

    @Inject
    private InstanceGroupV4RequestToInstanceGroupConverter instanceGroupConverter;

    @Inject
    private NetworkV4RequestToNetworkConverter networkConverter;

    @Inject
    private CredentialV4RequestToCredentialConverter credentialConverter;

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.nginx.port:9443}")
    private Integer nginxPort;

    @Override
    public Stack convert(CreateFreeIpaRequest source) {
        Stack stack = new Stack();
        stack.setName(source.getName());
        stack.setCreated(System.currentTimeMillis());
        stack.setGatewayport(source.getGatewayPort() != null ? source.getGatewayPort() : nginxPort);
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.PROVISION_REQUESTED));
        stack.setAvailabilityZone(getAvailabilityZone(Optional.ofNullable(source.getPlacement())));
        updateCloudPlatformAndRelatedFields(source, stack);
        stack.setStackAuthentication(stackAuthenticationConverter.convert(source.getAuthentication()));
        stack.setInstanceGroups(convertInstanceGroups(source, stack));
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            stack.setNetwork(networkConverter.convert(source.getNetwork()));
        }
        stack.setCredential(credentialConverter.convert(source.getCredential()));
        stack.setOwner(source.getOwner());
        return stack;
    }

    private String getAvailabilityZone(Optional<PlacementSettingsV4Request> placement) {
        return placement.map(PlacementSettingsV4Request::getAvailabilityZone).orElse(null);
    }

    private void updateCloudPlatformAndRelatedFields(CreateFreeIpaRequest source, Stack stack) {
        String cloudPlatform = source.getCredential().getCloudPlatform();
        source.setCloudPlatform(CloudPlatform.valueOf(cloudPlatform));
        stack.setRegion(getRegion(source, cloudPlatform));
        stack.setCloudPlatform(cloudPlatform);
        stack.setTags(getTags(source, cloudPlatform));
        stack.setPlatformvariant(cloudPlatform);
    }

    private String getRegion(CreateFreeIpaRequest source, String cloudPlatform) {
        if (source.getPlacement() == null) {
            return null;
        }
        if (isEmpty(source.getPlacement().getRegion())) {
            Map<Platform, Region> regions = Maps.newHashMap();
            if (isNoneEmpty(defaultRegions)) {
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

    private Json getTags(CreateFreeIpaRequest source, String cloudPlatform) {
        try {
            TagsV4Request tags = source.getTags();
            if (tags == null) {
                return new Json(new StackTags(new HashMap<>(), new HashMap<>(), getDefaultTags(source, cloudPlatform)));
            }
            return new Json(new StackTags(tags.getUserDefined(), tags.getApplication(), getDefaultTags(source, cloudPlatform)));
        } catch (Exception ignored) {
            throw new BadRequestException("Failed to convert dynamic tags.");
        }
    }

    private Map<String, String> getDefaultTags(CreateFreeIpaRequest source, String cloudPlatform) {
        Map<String, String> result = new HashMap<>();
        try {
            result.putAll(costTaggingService.prepareDefaultTags(source.getOwner(), result, cloudPlatform));
        } catch (Exception e) {
            LOGGER.debug("Exception during reading default tags.", e);
        }
        return result;
    }

    private Set<InstanceGroup> convertInstanceGroups(CreateFreeIpaRequest source, Stack stack) {
        if (source.getInstanceGroups() == null) {
            return null;
        }
        Set<InstanceGroup> convertedSet = new HashSet<>();
        source.getInstanceGroups().stream()
                .map(ig -> {
                    ig.setCloudPlatform(source.getCloudPlatform());
                    return instanceGroupConverter.convert(ig);
                })
                .forEach(ig -> {
                    ig.setStack(stack);
                    convertedSet.add(ig);
                });
        return convertedSet;
    }
}
