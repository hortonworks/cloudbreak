package com.sequenceiq.freeipa.service.migration;

import static java.util.function.Predicate.not;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationEvent;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class MultiAzMigrationRebalanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationRebalanceService.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    /**
     * Returns the non-primary gateway instances that have to be recreated. When the primary gateway is recreated, every instance has to be recreated,
     * otherwise only the minimum number of instances are selected that need to be moved to achieve an even distribution across the availability zones
     * of the environment, e.g. 3 instances in a single AZ with 2 AZs configured on the environment require only 1 instance to be moved for a 2:1 split.
     * When the primary gateway is not in any of the availability zones of the environment, its location is unknown, so every instance is recreated.
     */
    public Set<String> getNonPrimaryGwInstanceIdsToRecreate(MultiAzMigrationEvent event) {
        if (event.shouldRecreatePrimaryGw()) {
            LOGGER.info("The primary gateway is recreated, so all non-primary gateway instances need to be recreated as well.");
            return event.getNonPrimaryGwInstanceIdsToRecreate();
        }
        Stack stack = stackService.getStackById(event.getResourceId());
        Set<String> targetAzs = cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn()).getNetwork()
                .getAvailabilityZones(CloudPlatform.valueOf(stack.getCloudPlatform())).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<InstanceMetaData> instances = instanceMetaDataService.getNotTerminatedByInstanceIds(stack.getId(), event.getInstanceIds());
        String currentAzs = instances.stream()
                .map(instance -> instance.getInstanceId() + "=" + instance.getAvailabilityZone())
                .sorted()
                .collect(Collectors.joining(", "));
        boolean primaryGwInTargetAzs = instances.stream()
                .anyMatch(instance -> event.getPrimaryGwInstanceId().equals(instance.getInstanceId()) && isInAnyAz(instance, targetAzs));
        if (!primaryGwInTargetAzs) {
            LOGGER.warn("The primary gateway {} is not in any of the availability zones {} of the environment (current availability zones: {}), " +
                    "so all non-primary gateway instances need to be recreated.", event.getPrimaryGwInstanceId(), targetAzs, currentAzs);
            return event.getNonPrimaryGwInstanceIdsToRecreate();
        }
        int minInstanceCountPerAz = instances.size() / targetAzs.size();
        int azCountWithExtraInstance = instances.size() % targetAzs.size();
        Set<String> instanceIdsToRecreate = Stream.concat(
                        getInstanceIdsOutsideOfAzs(instances, targetAzs, event.getPrimaryGwInstanceId()),
                        getExcessInstanceIdsInAzs(instances, targetAzs, event.getPrimaryGwInstanceId(), minInstanceCountPerAz, azCountWithExtraInstance))
                .collect(Collectors.toSet());
        LOGGER.info("Non-primary gateway instances {} need to be recreated to achieve an even distribution of {} instances (current availability zones: {}) " +
                        "across {} availability zones {}: {} instances per availability zone, one more in the {} most used availability zones.",
                instanceIdsToRecreate, instances.size(), currentAzs, targetAzs.size(), targetAzs, minInstanceCountPerAz, azCountWithExtraInstance);
        return instanceIdsToRecreate;
    }

    private Stream<String> getInstanceIdsOutsideOfAzs(Set<InstanceMetaData> instances, Set<String> targetAzs, String primaryGwInstanceId) {
        return instances.stream()
                .filter(instance -> !isInAnyAz(instance, targetAzs))
                .map(InstanceMetaData::getInstanceId)
                .filter(not(primaryGwInstanceId::equals));
    }

    /**
     * The most used AZs are allowed to keep one more instance than the rest, when the instances can not be split evenly. Every used AZ keeps at least
     * one instance this way, so the primary gateway never needs to be moved.
     */
    private Stream<String> getExcessInstanceIdsInAzs(Set<InstanceMetaData> instances, Set<String> targetAzs, String primaryGwInstanceId,
            int minInstanceCountPerAz, int azCountWithExtraInstance) {
        List<List<InstanceMetaData>> instancesPerAzByUsage = instances.stream()
                .filter(instance -> isInAnyAz(instance, targetAzs))
                .collect(Collectors.groupingBy(InstanceMetaData::getAvailabilityZone))
                .values().stream()
                .sorted(Comparator.<List<InstanceMetaData>>comparingInt(List::size).reversed())
                .toList();
        return IntStream.range(0, instancesPerAzByUsage.size())
                .boxed()
                .flatMap(index -> {
                    List<InstanceMetaData> azInstances = instancesPerAzByUsage.get(index);
                    int targetInstanceCount = index < azCountWithExtraInstance ? minInstanceCountPerAz + 1 : minInstanceCountPerAz;
                    return azInstances.stream()
                            .map(InstanceMetaData::getInstanceId)
                            .filter(not(primaryGwInstanceId::equals))
                            .sorted()
                            .limit(Math.max(0, azInstances.size() - targetInstanceCount));
                });
    }

    private static boolean isInAnyAz(InstanceMetaData instance, Set<String> availabilityZones) {
        return instance.getAvailabilityZone() != null && availabilityZones.contains(instance.getAvailabilityZone());
    }
}
