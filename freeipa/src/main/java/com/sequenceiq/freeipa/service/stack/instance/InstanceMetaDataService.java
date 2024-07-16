package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.common.model.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;
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

    @Inject
    private ImageService imageService;

    @Inject
    private Clock clock;

    public void saveInstanceRequests(Stack stack, List<Group> groups) {
        Set<InstanceGroup> instanceGroups = stack.getInstanceGroups();
        Json image = new Json(imageService.getCloudImageByStackId(stack.getId()));
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
                    instanceMetaData.setImage(image);
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

    public Set<InstanceMetaData> getByInstanceIds(Long stackId, Iterable<String> instanceIds) {
        return instanceMetaDataRepository.findAllByInstanceIdIn(stackId, instanceIds);
    }

    public Set<InstanceMetaData> getNotTerminatedByInstanceIds(Long stackId, Iterable<String> instanceIds) {
        return instanceMetaDataRepository.findAllNotTerminatedByInstanceIdIn(stackId, instanceIds);
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

    public Stack saveInstanceAndGetUpdatedStack(Stack stack, List<CloudInstance> cloudInstances, List<InstanceMetaData> instancesToRemove) {
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        Json image = new Json(imageService.getCloudImageByStackId(stack.getId()));
        DetailedEnvironmentResponse environment = measure(() -> cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn()),
                LOGGER, "Environment properties were queried under {} ms for environment {}", stack.getEnvironmentCrn());
        Map<String, List<CloudInstance>> instancesPerGroup = cloudInstances.stream()
                .collect(Collectors.groupingBy(cloudInstance -> cloudInstance.getTemplate().getGroupName()));
        Iterator<InstanceMetaData> instanceIdsToRemoveIterator = instancesToRemove.iterator();
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
                    instanceMetaData.setImage(image);
                    if (instanceIdsToRemoveIterator.hasNext()) {
                        InstanceMetaData nextInstanceMetadata = instanceIdsToRemoveIterator.next();
                        instanceMetaData.setAvailabilityZone(nextInstanceMetadata.getAvailabilityZone());
                        instanceMetaData.setSubnetId(nextInstanceMetadata.getSubnetId());
                        LOGGER.debug("Instance metadata found with id: {}, the subnet and AZ are set to {}/{} for the new instance metadata with private id: {}",
                                nextInstanceMetadata.getInstanceId(), nextInstanceMetadata.getAvailabilityZone(), nextInstanceMetadata.getSubnetId(),
                                privateId);
                    }
                    if (StringUtils.isBlank(instanceMetaData.getSubnetId()) && !subnetAzMap.isEmpty()) {
                        LOGGER.debug("Calculate new subnet and AZ for private id: {}", privateId);
                        Map<String, String> filteredSubnetsByLeastUsedAz = multiAzCalculatorService.filterSubnetByLeastUsedAz(instanceGroup, subnetAzMap);
                        multiAzCalculatorService.updateSubnetIdForSingleInstanceIfEligible(filteredSubnetsByLeastUsedAz, currentSubnetUsage, instanceMetaData,
                                instanceGroup);
                    } else if (StringUtils.isNoneBlank(instanceMetaData.getSubnetId())) {
                        LOGGER.debug("Subnet and AZ calculation skipped, because the AZ/subnet already set: {}/{}",
                                instanceMetaData.getAvailabilityZone(), instanceMetaData.getSubnetId());
                    } else {
                        LOGGER.debug("Subnet and AZ calculation skipped, because the subnetAzMap is empty");
                    }
                    instanceGroup.getInstanceMetaDataSet().add(instanceMetaData);
                    multiAzCalculatorService.populateAvailabilityZonesForInstances(stack, instanceGroup);
                    instanceMetaDataRepository.save(instanceMetaData);
                    LOGGER.debug("Saved InstanceMetaData: {}", instanceMetaData);
                }
            }
        }
        return stack;
    }

    public Set<InstanceMetaData> getNonPrimaryGwInstances(Set<InstanceMetaData> allInstances) {
        Set<InstanceMetaData> nonPgwInstanceMetadataSet = allInstances.stream()
                .filter(instanceMetaData -> InstanceMetadataType.GATEWAY_PRIMARY != instanceMetaData.getInstanceMetadataType())
                .collect(Collectors.toCollection(HashSet::new));
        LOGGER.debug("Non-primary gateway instances: {}", nonPgwInstanceMetadataSet);
        return nonPgwInstanceMetadataSet;
    }

    public InstanceMetaData getPrimaryGwInstance(Set<InstanceMetaData> allInstances) {
        InstanceMetaData primaryGateway = allInstances.stream()
                .filter(instanceMetaData -> InstanceMetadataType.GATEWAY_PRIMARY == instanceMetaData.getInstanceMetadataType())
                .findFirst().orElseThrow(() -> new BadRequestException("No primary Gateway found"));
        LOGGER.debug("Found primary gateway with instance id: [{}]", primaryGateway.getInstanceId());
        return primaryGateway;
    }

    public List<SubnetIdWithResourceNameAndCrn> findAllUsedSubnetsByEnvironmentCrn(String environmentCrn) {
        List<SubnetIdWithResourceNameAndCrn> usedSubnets = instanceMetaDataRepository.findAllUsedSubnetsByEnvironmentCrn(environmentCrn);
        LOGGER.info("All used subnets ({}) for environment: {}", usedSubnets, environmentCrn);
        return usedSubnets;
    }

    public void updateInstanceStatusOnUpscaleFailure(Set<InstanceMetaData> notDeletedInstanceMetaDataSet) {
        LOGGER.debug("Updating instance metadata status after failed upscale. Available instance metadata: {}", notDeletedInstanceMetaDataSet);
        Set<InstanceMetaData> instancesToUpdate = new HashSet<>();
        notDeletedInstanceMetaDataSet.forEach(im -> {
            if (StringUtils.isBlank(im.getInstanceId())) {
                im.setInstanceStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.TERMINATED);
                im.setTerminationDate(clock.getCurrentTimeMillis());
                instancesToUpdate.add(im);
            } else if (StringUtils.isAnyBlank(im.getPrivateIp(), im.getDiscoveryFQDN())
                    || com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED == im.getInstanceStatus()) {
                im.setInstanceStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.FAILED);
                instancesToUpdate.add(im);
            }
        });
        if (instancesToUpdate.isEmpty()) {
            LOGGER.debug("There are no instances to update.");
        } else {
            LOGGER.warn("Updating the following instances status during failed upscale: {}", instancesToUpdate);
            saveAll(instancesToUpdate);
        }
    }

    public Set<InstanceMetaData> findAllInstancesForStack(Long stackId) {
        return instanceMetaDataRepository.findAllInStack(stackId);
    }
}
