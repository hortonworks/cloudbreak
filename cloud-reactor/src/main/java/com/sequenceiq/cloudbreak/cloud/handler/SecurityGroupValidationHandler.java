package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.view.PlatformResourceSecurityGroupFilterView;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;

/**
 * Platform-agnostic handler for {@link SecurityGroupValidationRequest}. Uses
 * {@link com.sequenceiq.cloudbreak.cloud.PlatformResources#securityGroups} through the
 * {@link PlatformResourceSecurityGroupFilterView#GROUP_IDS_KEY} filter so that providers whose SDK 400s on a missing
 * ID (AWS) return present IDs instead of an exception.
 *
 * <p>Classification uses one describe call: we query with only the {@code groupIds} filter (no network filter), then
 * decide locally which IDs are missing outright vs. which exist but sit in a different network. Querying with a
 * network filter would collapse the two into "missing" and lose the diagnostic.
 */
@Component
public class SecurityGroupValidationHandler implements CloudPlatformEventHandler<SecurityGroupValidationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupValidationHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<SecurityGroupValidationRequest> type() {
        return SecurityGroupValidationRequest.class;
    }

    @Override
    public void accept(Event<SecurityGroupValidationRequest> requestEvent) {
        LOGGER.debug("Received event: {}", requestEvent);
        SecurityGroupValidationRequest request = requestEvent.getData();
        SecurityGroupValidationResult result;
        try {
            Set<String> requestedIds = request.getSecurityGroupIds();
            if (requestedIds.isEmpty()) {
                LOGGER.debug("No security group IDs to validate, returning empty result");
                result = new SecurityGroupValidationResult(request.getResourceId(), Set.of(), Set.of());
            } else {
                CloudContext cloudContext = request.getCloudContext();
                Map<String, String> filters = Map.of(PlatformResourceSecurityGroupFilterView.GROUP_IDS_KEY, String.join(",", requestedIds));
                CloudSecurityGroups securityGroups = cloudPlatformConnectors.get(cloudContext.getPlatformVariant())
                        .platformResources()
                        .securityGroups(request.getExtendedCloudCredential(), Region.region(request.getRegion()), filters);
                result = classify(request.getResourceId(), requestedIds, request.getNetworkId(), securityGroups);
            }
            request.getResult().onNext(result);
            eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
        } catch (Exception e) {
            LOGGER.warn("Failed to handle SecurityGroupValidationRequest.", e);
            SecurityGroupValidationResult errorResult = new SecurityGroupValidationResult(e.getMessage(), e, request.getResourceId());
            request.getResult().onNext(errorResult);
            eventBus.notify(errorResult.selector(), new Event<>(requestEvent.getHeaders(), errorResult));
        }
    }

    private SecurityGroupValidationResult classify(Long resourceId, Set<String> requestedIds, String networkId, CloudSecurityGroups securityGroups) {
        Set<CloudSecurityGroup> found = securityGroups.getCloudSecurityGroupsResponses().values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        Set<String> foundIds = found.stream()
                .map(CloudSecurityGroup::getGroupId)
                .collect(Collectors.toSet());
        Set<String> foundIdsInNetwork = networkId == null
                ? Set.of()
                : found.stream()
                .filter(securityGroup -> networkId.equals(networkIdOf(securityGroup)))
                .map(CloudSecurityGroup::getGroupId)
                .collect(Collectors.toSet());
        Set<String> missing = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toSet());
        Set<String> notInNetwork = networkId == null
                ? Set.of()
                : requestedIds.stream()
                .filter(foundIds::contains)
                .filter(id -> !foundIdsInNetwork.contains(id))
                .collect(Collectors.toSet());
        if (!missing.isEmpty() || !notInNetwork.isEmpty()) {
            LOGGER.warn("Security group validation classification: missing={}, notInNetwork={}, networkId={}", missing, notInNetwork, networkId);
        } else {
            LOGGER.info("Security group validation passed for resource {}: checked [{}]",
                    resourceId, formatSecurityGroupsWithNetworkId(requestedIds, found));
        }
        return new SecurityGroupValidationResult(resourceId, missing, notInNetwork);
    }

    private String formatSecurityGroupsWithNetworkId(Set<String> requestedIds, Set<CloudSecurityGroup> found) {
        Map<String, String> networkIdByGroupId = found.stream()
                .collect(Collectors.toMap(CloudSecurityGroup::getGroupId, this::networkIdOf, (a, b) -> a));
        return requestedIds.stream()
                .sorted()
                .map(groupId -> {
                    String networkId = networkIdByGroupId.get(groupId);
                    return networkId == null ? groupId : groupId + " (network ID=" + networkId + ")";
                })
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns the network ID the security group belongs to, as reported by the provider in
     * {@link CloudSecurityGroup#getProperties()}. Compared against
     * {@link SecurityGroupValidationRequest#getNetworkId()} to classify wrong-network groups.
     * {@code null} means the provider did not supply network membership info. AWS currently
     * populates this as {@code vpcId}.
     */
    private String networkIdOf(CloudSecurityGroup securityGroup) {
        return Optional.ofNullable(securityGroup.getProperties())
                .map(properties -> properties.get("vpcId"))
                .map(Object::toString)
                .orElse(null);
    }
}
