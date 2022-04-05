package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.CloudResource.ATTRIBUTES;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class InstanceMetaDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetaDataService.class);

    @Inject
    private InstanceMetaDataRepository repository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    @Inject
    private ResourceRetriever resourceRetriever;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void updateInstanceStatus(Set<Long> instanceIds, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus,
            String statusReason) {
        try {
            LOGGER.info("Update {} status to {} with statusReason {}", instanceIds, newStatus, statusReason);
            transactionService.requiresNew(() -> {
                int modifiedRows = repository.updateStatusIfNotTerminated(instanceIds, newStatus, statusReason);
                LOGGER.debug("{} row was updated", modifiedRows);
                return modifiedRows;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Can't update instance status", e);
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public void updateInstanceStatus(final InstanceMetaData instanceMetaData, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus) {
        updateInstanceStatus(instanceMetaData, newStatus, null);
    }

    public void updateInstanceStatus(final InstanceMetaData instanceMetaData, com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus newStatus,
            String statusReason) {
        try {
            LOGGER.info("Update {} status to {} with statusReason {}", instanceMetaData.getInstanceId(), newStatus, statusReason);
            transactionService.requiresNew(() -> {
                int modifiedRows = repository.updateStatusIfNotTerminated(instanceMetaData.getId(), newStatus, statusReason);
                LOGGER.debug("{} row was updated", modifiedRows);
                return modifiedRows;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Can't update instance status", e);
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public Stack saveInstanceAndGetUpdatedStack(Stack stack, Map<String, Integer> hostGroupsWithInstanceCountToCreate,
            Map<String, Set<String>> hostGroupWithHostnames, boolean save, boolean repair, NetworkScaleDetails networkScaleDetails) {
        LOGGER.info("Get updated stack with instance count ({}) and hostnames: {} and save: ({})", hostGroupsWithInstanceCountToCreate, hostGroupWithHostnames,
                save);
        DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(stack.getEnvironmentCrn());
        Map<String, String> subnetAzPairs = multiAzCalculatorService.prepareSubnetAzMap(environment);
        String stackSubnetId = getStackSubnetIdIfExists(stack);
        String stackAz = stackSubnetId == null ? null : subnetAzPairs.get(stackSubnetId);
        long privateId = getFirstValidPrivateId(stack.getInstanceGroupsAsList());
        for (Map.Entry<String, Integer> hostGroupWithInstanceCount : hostGroupsWithInstanceCountToCreate.entrySet()) {
            String hostGroup = hostGroupWithInstanceCount.getKey();
            Integer instanceToCreate = hostGroupWithInstanceCount.getValue();
            Iterator<String> hostNameIterator = getHostNameIterator(hostGroupWithHostnames.get(hostGroup));
            for (int i = 0; i < instanceToCreate; i++) {
                InstanceGroup instanceGroup = getInstanceGroup(stack.getInstanceGroups(), hostGroup);
                if (instanceGroup != null) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(privateId++);
                    instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaData.setVariant(stack.getPlatformVariant());
                    if (hostNameIterator.hasNext()) {
                        String hostName = hostNameIterator.next();
                        repository.findHostInStack(stack.getId(), hostName).ifPresent(existingHost -> {
                            throw new CloudbreakServiceException("There is an existing host with the same FQDN. It can happen if you retried a failed repair. " +
                                    "Please start the repairing process again instead of retry.");
                        });
                        LOGGER.info("We have hostname to be allocated: {}, set it to this instanceMetadata: {}", hostName, instanceMetaData);
                        instanceMetaData.setDiscoveryFQDN(hostName);
                        subnetAzPairs = getSubnetAzPairsFilteredByHostNameIfRepair(environment, stack, repair, instanceGroup.getGroupName(), hostName);
                    }
                    Map<String, String> filteredSubnetsByLeastUsedAz = networkScaleDetails == null
                            ? multiAzCalculatorService.filterSubnetByLeastUsedAz(instanceGroup, subnetAzPairs)
                            : subnetAzPairs;
                    prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(instanceGroup, instanceMetaData, filteredSubnetsByLeastUsedAz, stackSubnetId,
                            stackAz, networkScaleDetails);
                    if (save) {
                        repository.save(instanceMetaData);
                    }
                    instanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
                }
            }
        }
        return stack;
    }

    private Iterator<String> getHostNameIterator(Set<String> hostNames) {
        Iterator<String> hostNameIterator;
        if (hostNames != null) {
            hostNameIterator = hostNames.iterator();
        } else {
            hostNameIterator = Collections.emptyIterator();
        }
        return hostNameIterator;
    }

    private long getFirstValidPrivateId(List<InstanceGroup> instanceGroups) {
        LOGGER.debug("Get first valid PrivateId of instanceGroups");
        long id = instanceGroups.stream()
                .flatMap(ig -> ig.getAllInstanceMetaData().stream())
                .map(InstanceMetaData::getPrivateId)
                .filter(Objects::nonNull)
                .map(i -> i + 1)
                .max(Long::compare)
                .orElse(0L);
        LOGGER.debug("First valid privateId: {}", id);
        return id;
    }

    @VisibleForTesting
    String getAzFromDiskOrNullIfRepair(Stack stack, boolean repair, String instanceGroup, String hostname) {
        String availabilityZone = null;
        if (repair) {
            ResourceType resourceType = getSupportedReattachableDiskType(stack);
            if (resourceType != null) {
                List<CloudResource> reattachableDiskResources = resourceRetriever.findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus.DETACHED,
                        resourceType, stack.getId(), instanceGroup);
                Optional<CloudResource> reattachableDiskResource = reattachableDiskResources.stream()
                        .filter(d -> d.getParameter(ATTRIBUTES, VolumeSetAttributes.class).getDiscoveryFQDN().equals(hostname))
                        .findFirst();
                if (reattachableDiskResource.isPresent()) {
                    VolumeSetAttributes volumeSetAttributes = reattachableDiskResource.get().getParameter(ATTRIBUTES, VolumeSetAttributes.class);
                    availabilityZone = volumeSetAttributes.getAvailabilityZone();
                    LOGGER.debug("Found AZ for the {}: {}", resourceType, availabilityZone);
                } else {
                    LOGGER.debug("Cannot find {} for {} in instanceGroup of {}", resourceType, hostname, instanceGroup);
                }
            }
        }
        return availabilityZone;
    }

    private ResourceType getSupportedReattachableDiskType(Stack stack) {
        if (stack.getCloudPlatform().equalsIgnoreCase(CloudPlatform.AWS.name())) {
            return ResourceType.AWS_VOLUMESET;
        }
        LOGGER.debug("Cloudbreak does not support the disk reattachment for {}", stack.getCloudPlatform());
        return null;
    }

    private Map<String, String> getSubnetAzPairsFilteredByHostNameIfRepair(DetailedEnvironmentResponse environment, Stack stack, boolean repair,
            String instanceGroup, String hostname) {
        String az = getAzFromDiskOrNullIfRepair(stack, repair, instanceGroup, hostname);
        return multiAzCalculatorService.prepareSubnetAzMap(environment, az);
    }

    private DetailedEnvironmentResponse getDetailedEnvironmentResponse(String environmentCrn) {
        return measure(() ->
                        ThreadBasedUserCrnProvider.doAsInternalActor(
                                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                                () -> environmentClientService.getByCrn(environmentCrn)),
                LOGGER,
                "Get Environment from Environment service took {} ms");
    }

    private String getStackSubnetIdIfExists(Stack stack) {
        return Optional.ofNullable(stack.getNetwork())
                .map(Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get("subnetId"))
                .map(Object::toString)
                .orElse(null);
    }

    private void prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(InstanceGroup instanceGroup, InstanceMetaData instanceMetaData,
            Map<String, String> subnetAzPairs, String stackSubnetId, String stackAz, NetworkScaleDetails networkScaleDetails) {
        multiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, instanceGroup, instanceMetaData, networkScaleDetails);
        if (Strings.isNullOrEmpty(instanceMetaData.getSubnetId()) && Strings.isNullOrEmpty(instanceMetaData.getAvailabilityZone())) {
            instanceMetaData.setSubnetId(stackSubnetId);
            instanceMetaData.setAvailabilityZone(stackAz);
        }
        instanceMetaData.setRackId(multiAzCalculatorService.determineRackId(instanceMetaData.getSubnetId(), instanceMetaData.getAvailabilityZone()));
    }

    public void saveInstanceRequests(Stack stack, List<Group> groups) {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        for (Group group : groups) {
            InstanceGroup instanceGroup = getInstanceGroup(instanceGroups, group.getName());
            List<InstanceMetaData> existingInGroup = repository.findAllByInstanceGroupAndInstanceStatus(instanceGroup,
                    com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                boolean exists = existingInGroup.stream().anyMatch(i -> i.getPrivateId().equals(instanceTemplate.getPrivateId()));
                if (com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus() && !exists) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(instanceTemplate.getPrivateId());
                    instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaData.setVariant(stack.getPlatformVariant());
                    repository.save(instanceMetaData);
                }
            }
        }
    }

    public Set<InstanceMetaData> unusedInstancesInInstanceGroupByName(Long stackId, String instanceGroupName) {
        return repository.findUnusedHostsInInstanceGroup(stackId, instanceGroupName);
    }

    public Set<InstanceMetaData> getAllInstanceMetadataByStackId(Long stackId) {
        return repository.findAllInStack(stackId);
    }

    public Set<InstanceMetaData> getNotDeletedAndNotZombieInstanceMetadataByStackId(Long stackId) {
        return repository.findAllInStack(stackId)
                .stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider() && !metaData.isZombie())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getNotDeletedInstanceMetadataByStackId(Long stackId) {
        return repository.findAllInStack(stackId)
                .stream()
                .filter(metaData -> !metaData.isTerminated() && !metaData.isDeletedOnProvider())
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getReachableInstanceMetadataByStackId(Long stackId) {
        return repository.findAllInStack(stackId)
                .stream()
                .filter(InstanceMetaData::isReachable)
                .collect(Collectors.toSet());
    }

    public Set<InstanceMetaData> getAllInstanceMetadataWithoutInstanceGroupByStackId(Long stackId) {
        return repository.findAllWithoutInstanceGroupInStack(stackId);
    }

    private InstanceGroup getInstanceGroup(Iterable<InstanceGroup> instanceGroups, String groupName) {
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (groupName.equalsIgnoreCase(instanceGroup.getGroupName())) {
                return instanceGroup;
            }
        }
        return null;
    }

    public Optional<InstanceMetaData> getPrimaryGatewayInstanceMetadata(long stackId) {
        try {
            return repository.getPrimaryGatewayInstanceMetadata(stackId);
        } catch (AccessDeniedException ignore) {
            LOGGER.debug("No primary gateway for stack [{}]", stackId);
            return Optional.empty();
        }
    }

    public Optional<InstanceMetaData> getTerminatedInstanceMetadataWithInstanceIdByFQDNOrdered(long stackId, String hostName) {
        try {
            List<InstanceMetaData> result = repository.getTerminatedInstanceMetadataWithInstanceIdByFQDNOrdered(stackId, hostName, PageRequest.of(0, 1));
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (AccessDeniedException ignore) {
            LOGGER.debug("Cannot fetch last terminated instance metadata for stack [{}] and hostname [{}]", stackId, hostName);
            return Optional.empty();
        }
    }

    public Optional<InstanceMetaData> getLastTerminatedPrimaryGatewayInstanceMetadata(long stackId) {
        try {
            List<InstanceMetaData> result = repository.geTerminatedPrimaryGatewayInstanceMetadataOrdered(stackId, PageRequest.of(0, 1));
            return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
        } catch (AccessDeniedException ignore) {
            LOGGER.debug("Cannot fetch the terminated primary gateways for stack [{}]", stackId);
            return Optional.empty();
        }
    }

    public Set<InstanceMetaData> findNotTerminatedForStack(Long stackId) {
        return repository.findNotDeletedForStack(stackId);
    }

    public Set<InstanceMetaData> findNotTerminatedAndNotZombieForStack(Long stackId) {
        return repository.findNotTerminatedAndNotZombieForStack(stackId);
    }

    public List<InstanceMetaData> findNotTerminatedAsOrderedListForStack(Long stackId) {
        return repository.findNotTerminatedAsOrderedListForStack(stackId);
    }

    public InstanceMetaData save(InstanceMetaData instanceMetaData) {
        return repository.save(instanceMetaData);
    }

    public Set<InstanceMetaData> findNotTerminatedAndNotZombieForStackWithoutInstanceGroups(Long stackId) {
        return repository.findNotTerminatedAndNotZombieForStackWithoutInstanceGroups(stackId);
    }

    public Optional<InstanceMetaData> findByStackIdAndInstanceId(Long stackId, String instanceId) {
        return repository.findByStackIdAndInstanceId(stackId, instanceId);
    }

    public List<InstanceMetaData> findByStackIdAndInstanceIds(Long stackId, Collection<String> instanceIds) {
        return repository.findByStackIdAndInstanceIds(stackId, instanceIds);
    }

    public Optional<InstanceMetaData> findHostInStack(Long stackId, String hostName) {
        return repository.findHostInStack(stackId, hostName);
    }

    public Set<InstanceMetaData> findUnusedHostsInInstanceGroup(Long instanceGroupId) {
        return repository.findUnusedHostsInInstanceGroup(instanceGroupId);
    }

    public void delete(InstanceMetaData instanceMetaData) {
        repository.delete(instanceMetaData);
    }

    public void deleteAll(List<InstanceMetaData> instanceMetaData) {
        repository.deleteAll(instanceMetaData);
    }

    public Optional<InstanceMetaData> findNotTerminatedByPrivateAddress(Long stackId, String privateAddress) {
        return repository.findNotTerminatedByPrivateAddress(stackId, privateAddress);
    }

    public List<InstanceMetaData> findAliveInstancesInInstanceGroup(Long instanceGroupId) {
        return repository.findAliveInstancesInInstanceGroup(instanceGroupId);
    }

    public Iterable<InstanceMetaData> saveAll(Iterable<InstanceMetaData> instanceMetaData) {
        return repository.saveAll(instanceMetaData);
    }

    public Optional<String> getPrimaryGatewayDiscoveryFQDNByInstanceGroup(Long stackId, Long instanceGroupId) {
        return repository.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(stackId, instanceGroupId);
    }

    public Optional<String> getServerCertByStackId(Long stackId) {
        return repository.getServerCertByStackId(stackId);
    }

    public Optional<InstanceMetaData> findById(Long id) {
        return repository.findById(id);
    }

    public Set<StackInstanceCount> countByWorkspaceId(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        return repository.countByWorkspaceId(workspaceId, environmentCrn, stackTypes);
    }

    public StackInstanceCount countByStackId(Long stackId) {
        return repository.countByStackId(stackId);
    }

    public List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatus(InstanceGroup instanceGroup,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus status) {
        return repository.findAllByInstanceGroupAndInstanceStatus(instanceGroup, status);
    }

    public List<InstanceMetaData> findAllByInstanceGroupAndInstanceStatusOrdered(InstanceGroup instanceGroup,
            com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus status) {
        return repository.findAllByInstanceGroupAndInstanceStatusOrderByPrivateIdAsc(instanceGroup, status);
    }

    public Optional<InstanceMetaData> findByHostname(Long stackId, String hostName) {
        return repository.findHostInStack(stackId, hostName);
    }

    public Page<InstanceMetaData> getTerminatedInstanceMetaDataBefore(Long stackId, Long thresholdTerminationDate, PageRequest pageRequest) {
        return repository.findTerminatedInstanceMetadataByStackIdAndTerminatedBefore(stackId, thresholdTerminationDate, pageRequest);
    }
}
