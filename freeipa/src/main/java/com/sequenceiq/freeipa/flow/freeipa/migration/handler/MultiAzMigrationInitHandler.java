package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitResult;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@Component
public class MultiAzMigrationInitHandler extends ExceptionCatcherEventHandler<MultiAzMigrationInitHandlerRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationInitHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private EnvironmentService environmentService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MultiAzMigrationInitHandlerRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MultiAzMigrationInitHandlerRequest> event) {
        LOGGER.warn("Exception during multi-AZ migration DB initialization for stack {}: ", resourceId, e);
        return new MultiAzMigrationInitFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<MultiAzMigrationInitHandlerRequest> handlerEvent) {
        MultiAzMigrationInitHandlerRequest request = handlerEvent.getData();
        Long stackId = request.getResourceId();
        LOGGER.info("Starting multi-AZ migration DB initialization for stack: {}", stackId);

        String environmentCrn = stackService.getEnvironmentCrnByStackId(stackId);
        DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(environmentCrn);
        try {
            transactionService.required(() -> {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                Set<String> availabilityZones = environment.getNetwork().getAvailabilityZones(CloudPlatform.valueOf(stack.getCloudPlatform()));
                LOGGER.debug("Availability zones from environment for stack {}: {}", stack.getName(), availabilityZones);
                Map<String, CloudSubnet> subnetMetas = environment.getNetwork().getSubnetMetas();

                for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                    updateInstanceGroupAvailabilityZones(instanceGroup, availabilityZones, subnetMetas, stack);
                }

                stackUpdater.updateMultiAzEnabled(stack.getId(), true);

            });
        } catch (TransactionExecutionException e) {
            LOGGER.warn("Failed to update availability zones for stack {} during multi-AZ migration", stackId, e);
            return new MultiAzMigrationInitFailedEvent(stackId, e);
        }

        environmentService.setFreeIpaEnableMultiAz(environmentCrn);
        LOGGER.info("Multi-AZ migration DB initialization completed for stack: [{}]", stackId);

        return new MultiAzMigrationInitResult(stackId, request.getOperationId());
    }

    private void updateInstanceGroupAvailabilityZones(InstanceGroup instanceGroup, Set<String> targetAzs, Map<String, CloudSubnet> subnetMetas, Stack stack) {
        if (instanceGroup.getInstanceGroupNetwork() == null) {
            instanceGroup.setInstanceGroupNetwork(createInstanceGroupNetworkFromStack(stack));
        }

        Set<String> currentAzs = currentAvailabilityZones(instanceGroup);
        Set<String> targetSubnetIds = subnetMetas.entrySet().stream()
                .filter(e -> e.getValue() != null && targetAzs.contains(e.getValue().getAvailabilityZone()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Object> attributesMap = currentAttributesMap(instanceGroup);
        Set<String> currentSubnetIds = currentSubnetIdsFromAttributes(attributesMap);

        if (Objects.equals(targetAzs, currentAzs) && Objects.equals(targetSubnetIds, currentSubnetIds)) {
            LOGGER.debug("Skipping availability-zone update for instance group {} — already up to date (azs={}, subnetIds={}).",
                    instanceGroup.getGroupName(), targetAzs, targetSubnetIds);
        } else {
            attributesMap.put(NetworkConstants.SUBNET_IDS, List.copyOf(targetSubnetIds));
            attributesMap.put(NetworkConstants.AVAILABILITY_ZONES, targetAzs);
            instanceGroup.getInstanceGroupNetwork().setAttributes(new Json(attributesMap));
            Set<InstanceGroupAvailabilityZone> newZones = targetAzs.stream().map(az -> {
                InstanceGroupAvailabilityZone igAz = new InstanceGroupAvailabilityZone();
                igAz.setInstanceGroup(instanceGroup);
                igAz.setAvailabilityZone(az);
                return igAz;
            }).collect(Collectors.toSet());
            instanceGroup.getAvailabilityZones().clear();
            instanceGroup.getAvailabilityZones().addAll(newZones);
            instanceGroupService.save(instanceGroup);
            LOGGER.debug("Updated availability zones {} (subnetIds={}) for instance group {}", targetAzs, targetSubnetIds, instanceGroup.getGroupName());
        }
    }

    private InstanceGroupNetwork createInstanceGroupNetworkFromStack(Stack stack) {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        Network stackNetwork = stack.getNetwork();
        String cloudPlatform = Optional.ofNullable(stackNetwork).map(Network::getCloudPlatform).orElse(stack.getCloudPlatform());
        instanceGroupNetwork.setCloudPlatform(cloudPlatform);
        Map<String, Object> attributesMap = Optional.ofNullable(stackNetwork).map(Network::getAttributes).map(Json::getMap).map(m -> new HashMap<>(m))
                .orElse(new HashMap<>());
        instanceGroupNetwork.setAttributes(new Json(attributesMap));
        LOGGER.debug("Created instance group network (cloudPlatform={}, attrs={}) from stack network {}", cloudPlatform, attributesMap, stackNetwork);
        return instanceGroupNetwork;
    }

    private Set<String> currentAvailabilityZones(InstanceGroup instanceGroup) {
        if (instanceGroup.getAvailabilityZones() == null) {
            return Set.of();
        }
        return instanceGroup.getAvailabilityZones().stream()
                .map(InstanceGroupAvailabilityZone::getAvailabilityZone)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> currentAttributesMap(InstanceGroup instanceGroup) {
        Json attributes = instanceGroup.getInstanceGroupNetwork().getAttributes();
        return attributes != null && attributes.getMap() != null ? new HashMap<>(attributes.getMap()) : new HashMap<>();
    }

    private Set<String> currentSubnetIdsFromAttributes(Map<String, Object> attributesMap) {
        Object current = attributesMap.get(NetworkConstants.SUBNET_IDS);
        if (current instanceof Collection<?> collection) {
            return collection.stream()
                    .map(String.class::cast)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }
}
