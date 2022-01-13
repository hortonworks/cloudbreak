package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;

@Service
public class InstanceMetaDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetaDataService.class);

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private FreeIpaService freeIpaService;

    public void saveInstanceRequests(Stack stack, List<Group> groups) {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        for (Group group : groups) {
            InstanceGroup instanceGroup = getInstanceGroup(instanceGroups, group.getName());
            List<InstanceMetaData> existingInGroup = instanceMetaDataRepository.findAllByInstanceGroupAndInstanceStatus(instanceGroup,
                    com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED);
            for (CloudInstance cloudInstance : group.getInstances()) {
                InstanceTemplate instanceTemplate = cloudInstance.getTemplate();
                boolean exists = existingInGroup.stream().anyMatch(i -> i.getPrivateId().equals(instanceTemplate.getPrivateId()));
                if (InstanceStatus.CREATE_REQUESTED == instanceTemplate.getStatus() && !exists) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    instanceMetaData.setPrivateId(instanceTemplate.getPrivateId());
                    instanceMetaData.setInstanceStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaData.setVariant(stack.getPlatformvariant());
                    instanceMetaDataRepository.save(instanceMetaData);
                }
            }
        }
    }

    public void updateStatus(Stack stack, List<String> instanceIds, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus status) {
        Set<InstanceMetaData> allInstances = instanceMetaDataRepository.findAllInStack(stack.getId());
        allInstances.stream().filter(im -> instanceIds.contains(im.getInstanceId()))
                .forEach(im -> {
                    im.setInstanceStatus(status);
                    instanceMetaDataRepository.save(im);
                    LOGGER.info("Updated instanceId {} status to {}.", im.getInstanceId(), status);
                });
    }

    public Set<InstanceMetaData> findNotTerminatedForStack(Long stackId) {
        return instanceMetaDataRepository.findNotTerminatedForStack(stackId);
    }

    public InstanceMetaData save(InstanceMetaData instanceMetaData) {
        return instanceMetaDataRepository.save(instanceMetaData);
    }

    public Set<InstanceMetaData> getByInstanceId(Long stackId, String instanceId) {
        return instanceMetaDataRepository.findAllByInstanceIdIn(stackId, Set.of(instanceId));
    }

    public Set<InstanceMetaData> getByInstanceIds(Long stackId, Iterable<String> instanceIds) {
        return instanceMetaDataRepository.findAllByInstanceIdIn(stackId, instanceIds);
    }

    public Set<InstanceMetaData> getNotTerminatedByInstanceIds(Long stackId, Iterable<String> instanceIds) {
        return instanceMetaDataRepository.findAllNotTerminatedByInstanceIdIn(stackId, instanceIds);
    }

    public Optional<InstanceMetaData> getById(Long id) {
        return instanceMetaDataRepository.findById(id);
    }

    private InstanceGroup getInstanceGroup(Collection<InstanceGroup> instanceGroups, String groupName) {
        return instanceGroups.stream().filter(ig -> groupName.equalsIgnoreCase(ig.getGroupName())).findFirst().orElse(null);
    }

    public Iterable<InstanceMetaData> saveAll(Iterable<InstanceMetaData> allInstanceMetaData) {
        return instanceMetaDataRepository.saveAll(allInstanceMetaData);
    }

    public Optional<StackAuthenticationView> getStackAuthenticationViewByInstanceMetaDataId(Long instanceId) {
        return instanceMetaDataRepository.getStackAuthenticationViewByInstanceMetaDataId(instanceId);
    }

    public Stack saveInstanceAndGetUpdatedStack(Stack stack, List<CloudInstance> cloudInstances) {
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        DetailedEnvironmentResponse environment = measure(() -> cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn()),
                LOGGER, "Environment properties were queried under {} ms for environment {}", stack.getEnvironmentCrn());
        Map<String, List<CloudInstance>> instancesPerGroup = cloudInstances.stream()
                .collect(Collectors.groupingBy(cloudInstance -> cloudInstance.getTemplate().getGroupName()));
        for (Map.Entry<String, List<CloudInstance>> instancesPerGroupEntry : instancesPerGroup.entrySet()) {
            InstanceGroup instanceGroup = getInstanceGroup(stack.getInstanceGroups(), instancesPerGroupEntry.getKey());
            if (instanceGroup != null) {
                Map<String, String> subnetAzMap = multiAzCalculatorService.prepareSubnetAzMap(environment);
                Map<String, Integer> currentSubnetUsage = multiAzCalculatorService.calculateCurrentSubnetUsage(subnetAzMap, instanceGroup);
                for (CloudInstance cloudInstance : instancesPerGroupEntry.getValue()) {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    Long privateId = cloudInstance.getTemplate().getPrivateId();
                    instanceMetaData.setPrivateId(privateId);
                    instanceMetaData.setInstanceStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED);
                    instanceMetaData.setInstanceGroup(instanceGroup);
                    instanceMetaData.setDiscoveryFQDN(freeIpa.getHostname() + String.format("%d.", privateId) + freeIpa.getDomain());
                    Map<String, String> filteredSubnetsByLeastUsedAz = multiAzCalculatorService.filterSubnetByLeastUsedAz(instanceGroup, subnetAzMap);
                    multiAzCalculatorService.updateSubnetIdForSingleInstanceIfEligible(filteredSubnetsByLeastUsedAz, currentSubnetUsage, instanceMetaData,
                            instanceGroup);
                    instanceMetaDataRepository.save(instanceMetaData);
                    LOGGER.debug("Saved InstanceMetaData: {}", instanceMetaData);
                    instanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
                }
            }
        }
        return stack;
    }

}
