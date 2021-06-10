package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class MetadataSetupService {

    private static final String DEFAULT_RACK = "default-rack";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupService.class);

    @Inject
    private ImageService imageService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private Clock clock;

    @Inject
    private TransactionService transactionService;

    public void cleanupRequestedInstances(Long stackId) {
        try {
            transactionService.required(() -> {
                Set<InstanceMetaData> allInstanceMetadataByStackId = instanceMetaDataService.findNotTerminatedForStack(stackId);
                List<InstanceMetaData> requestedInstances = allInstanceMetadataByStackId.stream()
                        .filter(instanceMetaData -> InstanceStatus.REQUESTED.equals(instanceMetaData.getInstanceStatus()))
                        .collect(Collectors.toList());
                for (InstanceMetaData inst : requestedInstances) {
                    inst.setTerminationDate(clock.getCurrentTimeMillis());
                    inst.setInstanceStatus(InstanceStatus.TERMINATED);
                }
                instanceMetaDataService.saveAll(requestedInstances);
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public void cleanupRequestedInstances(Stack stack, String instanceGroupName) {
        try {
            transactionService.required(() -> {
                Optional<InstanceGroup> ig = instanceGroupService.findOneWithInstanceMetadataByGroupNameInStack(stack.getId(), instanceGroupName);
                if (ig.isPresent()) {
                    List<InstanceMetaData> requestedInstances = instanceMetaDataService.findAllByInstanceGroupAndInstanceStatus(ig.get(),
                            InstanceStatus.REQUESTED);
                    for (InstanceMetaData inst : requestedInstances) {
                        inst.setTerminationDate(clock.getCurrentTimeMillis());
                        inst.setInstanceStatus(InstanceStatus.TERMINATED);
                    }
                    instanceMetaDataService.saveAll(requestedInstances);
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public int saveInstanceMetaData(Stack stack, Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        try {
            LOGGER.info("Save instance metadata for stack: {}", stack.getName());
            int newInstances = 0;
            Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
            boolean primaryIgSelected = allInstanceMetadata.stream().anyMatch(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);
            Json imageJson = new Json(imageService.getImage(stack.getId()));

            Map<String, InstanceGroup> instanceGroups = instanceGroupService.findByStackId(stack.getId())
                    .stream()
                    .collect(Collectors.toMap(InstanceGroup::getGroupName, Function.identity()));

            for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
                CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
                CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
                InstanceTemplate template = cloudInstance.getTemplate();
                Long privateId = template.getPrivateId();
                String instanceId = cloudInstance.getInstanceId();
                InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
                if (instanceMetaDataEntry.getInstanceId() == null && cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == CREATED) {
                    newInstances++;
                }
                // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
                InstanceGroup ig = instanceMetaDataEntry.getInstanceGroup();
                String group = ig == null ? template.getGroupName() : ig.getGroupName();
                InstanceGroup instanceGroup = instanceGroups.get(group);
                instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
                instanceMetaDataEntry.setPublicIp(md.getPublicIp());
                instanceMetaDataEntry.setSshPort(md.getSshPort());
                instanceMetaDataEntry.setLocalityIndicator(md.getLocalityIndicator());
                instanceMetaDataEntry.setInstanceGroup(instanceGroup);
                instanceMetaDataEntry.setInstanceId(instanceId);
                instanceMetaDataEntry.setPrivateId(privateId);
                instanceMetaDataEntry.setStartDate(clock.getCurrentTimeMillis());
                instanceMetaDataEntry.setSubnetId(cloudInstance.getStringParameter(CloudInstance.SUBNET_ID));
                instanceMetaDataEntry.setAvailabilityZone(cloudInstance.getStringParameter(CloudInstance.AVAILABILITY_ZONE));
                instanceMetaDataEntry.setRackId(determineRackId(instanceMetaDataEntry.getSubnetId(), instanceMetaDataEntry.getAvailabilityZone()));
                instanceMetaDataEntry.setInstanceName(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME));
                instanceMetaDataEntry.setServer(Boolean.FALSE);
                instanceMetaDataEntry.setLifeCycle(InstanceLifeCycle.fromCloudInstanceLifeCycle(md.getLifeCycle()));
                if (instanceMetaDataEntry.getInstanceMetadataType() == null) {
                    if (ig != null) {
                        if (InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())) {
                            if (!primaryIgSelected) {
                                primaryIgSelected = true;
                                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                                instanceMetaDataEntry.setServer(Boolean.TRUE);
                                LOGGER.info("Primary gateway is not selected, let's select this instance: {}", instanceMetaDataEntry.getInstanceId());
                            } else {
                                LOGGER.info("Primary gateway was selected");
                                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
                            }
                        } else {
                            LOGGER.info("Instance is a core instance: {}", instanceMetaDataEntry.getInstanceId());
                            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                        }
                    } else {
                        LOGGER.info("Instance group is null, instance will be a core instance: {}", instanceMetaDataEntry.getInstanceId());
                        instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                    }
                }
                if (status != null) {
                    if (cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == TERMINATED) {
                        instanceMetaDataEntry.setInstanceStatus(InstanceStatus.TERMINATED);
                    } else {
                        instanceMetaDataEntry.setInstanceStatus(status);
                        instanceMetaDataEntry.setImage(imageJson);
                    }
                }
                instanceMetaDataService.save(instanceMetaDataEntry);
            }
            return newInstances;
        } catch (CloudbreakImageNotFoundException | IllegalArgumentException ex) {
            throw new CloudbreakServiceException("Instance metadata collection failed", ex);
        }
    }

    private String determineRackId(String subnetId, String availabilityZone) {
        return "/" +
                (isNullOrEmpty(availabilityZone) ?
                        (isNullOrEmpty(subnetId) ? DEFAULT_RACK : subnetId)
                        : availabilityZone);
    }

    private InstanceMetaData createInstanceMetadataIfAbsent(Iterable<InstanceMetaData> allInstanceMetadata, Long privateId, String instanceId) {
        if (privateId != null) {
            for (InstanceMetaData instanceMetaData : allInstanceMetadata) {
                if (Objects.equals(instanceMetaData.getPrivateId(), privateId)) {
                    return instanceMetaData;
                }
            }
        } else {
            for (InstanceMetaData instanceMetaData : allInstanceMetadata) {
                if (Objects.equals(instanceMetaData.getInstanceId(), instanceId)) {
                    return instanceMetaData;
                }
            }
        }
        return new InstanceMetaData();
    }

    public void saveLoadBalancerMetadata(Stack stack, Iterable<CloudLoadBalancerMetadata> cloudLoadBalancerMetadataList) {
        try {
            LOGGER.info("Save load balancer metadata for stack: {}", stack.getName());

            Set<LoadBalancer> allLoadBalancerMetadata = loadBalancerPersistenceService.findByStackId(stack.getId());

            for (CloudLoadBalancerMetadata cloudLoadBalancerMetadata : cloudLoadBalancerMetadataList) {
                LoadBalancer loadBalancerEntry = createLoadBalancerMetadataIfAbsent(allLoadBalancerMetadata,
                    stack, cloudLoadBalancerMetadata.getType());

                loadBalancerEntry.setDns(cloudLoadBalancerMetadata.getCloudDns());
                loadBalancerEntry.setHostedZoneId(cloudLoadBalancerMetadata.getHostedZoneId());
                loadBalancerEntry.setIp(cloudLoadBalancerMetadata.getIp());
                loadBalancerEntry.setType(cloudLoadBalancerMetadata.getType());
                String endpoint = loadBalancerConfigService.generateLoadBalancerEndpoint(stack);
                LOGGER.info("Saving load balancer endpoint as: {}", endpoint);
                loadBalancerEntry.setEndpoint(endpoint);

                loadBalancerPersistenceService.save(loadBalancerEntry);
            }
        } catch (Exception ex) {
            throw new CloudbreakServiceException("Load balancer metadata collection failed", ex);
        }
    }

    private LoadBalancer createLoadBalancerMetadataIfAbsent(Iterable<LoadBalancer> allLoadBalancerMetadata,
            Stack stack, LoadBalancerType type) {
        if (stack != null && type != null) {
            for (LoadBalancer loadBalancerMetadata : allLoadBalancerMetadata) {
                if (Objects.equals(stack.getId(), loadBalancerMetadata.getStack().getId()) &&
                        type == loadBalancerMetadata.getType()) {
                    return loadBalancerMetadata;
                }
            }
        }
        return new LoadBalancer();
    }

}
