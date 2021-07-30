package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.multiaz.MultiAzCalculatorService;
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

    public Stack saveInstanceAndGetUpdatedStack(Stack stack, List<CloudInstance> cloudInstances, boolean save, Set<String> hostNames) {
        LOGGER.info("Get updated stack with instances: {} and hostnames: {} and save: ({})", cloudInstances, hostNames, save);
        Map<String, String> subnetAzPairs = getSubnetAzPairs(stack.getEnvironmentCrn());
        String stackSubnetId = getStackSubnetIdIfExists(stack);
        String stackAz = stackSubnetId == null ? null : subnetAzPairs.get(stackSubnetId);
        Iterator<String> hostNameIterator = hostNames.iterator();
        for (CloudInstance cloudInstance : cloudInstances) {
            InstanceGroup instanceGroup = getInstanceGroup(stack.getInstanceGroups(), cloudInstance.getTemplate().getGroupName());
            if (instanceGroup != null) {
                InstanceMetaData instanceMetaData = new InstanceMetaData();
                instanceMetaData.setPrivateId(cloudInstance.getTemplate().getPrivateId());
                instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED);
                instanceMetaData.setInstanceGroup(instanceGroup);
                if (hostNameIterator.hasNext()) {
                    String hostName = hostNameIterator.next();
                    LOGGER.info("We have hostname to be allocated: {}, set it to this instanceMetadata: {}", hostName, instanceMetaData);
                    instanceMetaData.setDiscoveryFQDN(hostName);
                }
                prepareCloudInstanceSubnetAndAvailabilityZone(instanceGroup, cloudInstance, subnetAzPairs, stackSubnetId, stackAz);
                prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(cloudInstance, instanceMetaData);
                if (save) {
                    repository.save(instanceMetaData);
                }
                instanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
            }
        }
        return stack;
    }

    private Map<String, String> getSubnetAzPairs(String environmentCrn) {
        DetailedEnvironmentResponse environment = measure(() ->
                        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                                environmentClientService.getByCrn(environmentCrn)),
                LOGGER,
                "Get Environment from Environment service took {} ms");
        return multiAzCalculatorService.prepareSubnetAzMap(environment);
    }

    private String getStackSubnetIdIfExists(Stack stack) {
        return Optional.ofNullable(stack.getNetwork())
                .map(Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get("subnetId"))
                .map(Object::toString)
                .orElse(null);
    }

    private void prepareCloudInstanceSubnetAndAvailabilityZone(InstanceGroup instanceGroup, CloudInstance cloudInstance, Map<String, String> subnetAzPairs,
            String stackSubnetId, String stackAz) {
        cloudInstance.setSubnetId(null);
        cloudInstance.setAvailabilityZone(null);
        multiAzCalculatorService.calculateByRoundRobin(
                subnetAzPairs,
                instanceGroup,
                cloudInstance
        );
        if (Strings.isNullOrEmpty(cloudInstance.getSubnetId()) && Strings.isNullOrEmpty(cloudInstance.getAvailabilityZone())) {
            cloudInstance.setSubnetId(stackSubnetId);
            cloudInstance.setAvailabilityZone(stackAz);
        }
    }

    private void prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(CloudInstance cloudInstance, InstanceMetaData instanceMetaData) {
        String subnetId = cloudInstance.getSubnetId();
        String availabilityZone = cloudInstance.getAvailabilityZone();
        instanceMetaData.setSubnetId(subnetId);
        instanceMetaData.setAvailabilityZone(availabilityZone);
        instanceMetaData.setRackId(multiAzCalculatorService.determineRackId(subnetId, availabilityZone));
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
        return repository.findNotTerminatedForStack(stackId);
    }

    public InstanceMetaData save(InstanceMetaData instanceMetaData) {
        return repository.save(instanceMetaData);
    }

    public Set<InstanceMetaData> findNotTerminatedForStackWithoutInstanceGroups(Long stackId) {
        return repository.findNotTerminatedForStackWithoutInstanceGroups(stackId);
    }

    public Optional<InstanceMetaData> findByStackIdAndInstanceId(Long stackId, String instanceId) {
        return repository.findByStackIdAndInstanceId(stackId, instanceId);
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

    public Optional<InstanceMetaData> findByHostname(Long stackId, String hostName) {
        return repository.findHostInStack(stackId, hostName);
    }

}
