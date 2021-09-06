package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;

import java.util.Collection;
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
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigConverter;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class MetadataSetupService {

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
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private Clock clock;

    @Inject
    private TransactionService transactionService;

    @Inject
    private LoadBalancerConfigConverter loadBalancerConfigConverter;

    public void handleRepairFail(Long stackId, Set<String> hostNames) {
            for (String hostName : hostNames) {
                try {
                    transactionService.required(() -> {
                        Optional<InstanceMetaData> instance = instanceMetaDataService.findByHostname(stackId, hostName);
                        instance.ifPresentOrElse(instanceMetaData -> {
                            if (InstanceStatus.REQUESTED.equals(instanceMetaData.getInstanceStatus())) {
                                restorePreviousTerminatedInstanceMetadata(stackId, hostName);
                                instanceMetaData.setTerminationDate(clock.getCurrentTimeMillis());
                                instanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                                instanceMetaDataService.save(instanceMetaData);
                            }
                        }, () -> restorePreviousTerminatedInstanceMetadata(stackId, hostName));
                    });
                } catch (TransactionExecutionException e) {
                    throw new TransactionRuntimeExecutionException(e);
                }
            }
    }

    private void restorePreviousTerminatedInstanceMetadata(Long stackId, String hostName) {
        Optional<InstanceMetaData> lastTerminatedInstanceMetadataWithInstanceIdByFQDN =
                instanceMetaDataService.getTerminatedInstanceMetadataWithInstanceIdByFQDNOrdered(stackId, hostName);
        lastTerminatedInstanceMetadataWithInstanceIdByFQDN.ifPresent(instanceMetaData -> {
            instanceMetaData.setTerminationDate(null);
            instanceMetaData.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
            instanceMetaDataService.save(instanceMetaData);
        });
    }

    public void cleanupRequestedInstancesWithoutFQDN(Stack stack, String instanceGroupName) {
        try {
            transactionService.required(() -> {
                Optional<InstanceGroup> ig = instanceGroupService.findOneWithInstanceMetadataByGroupNameInStack(stack.getId(), instanceGroupName);
                if (ig.isPresent()) {
                    List<InstanceMetaData> requestedInstances = instanceMetaDataService.findAllByInstanceGroupAndInstanceStatus(ig.get(),
                            InstanceStatus.REQUESTED);
                    List<InstanceMetaData> requestedInstancesWithoutFQDN =
                            requestedInstances.stream().filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() == null).collect(Collectors.toList());
                    LOGGER.info("Set requested instances without FQDN to terminated: {}", requestedInstancesWithoutFQDN);
                    for (InstanceMetaData inst : requestedInstancesWithoutFQDN) {
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

            Optional<InstanceMetaData> terminatedPrimaryGwWhichShouldBeRestored = Optional.empty();
            if (!primaryIgSelected) {
                terminatedPrimaryGwWhichShouldBeRestored = instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(stack.getId());
                LOGGER.info("Terminated primary GW which should be restored: {}", terminatedPrimaryGwWhichShouldBeRestored);
            }

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
                setupFromCloudInstanceMetadata(md, instanceMetaDataEntry);
                setupFromCloudInstance(cloudInstance, instanceMetaDataEntry);
                instanceMetaDataEntry.setInstanceGroup(instanceGroup);
                instanceMetaDataEntry.setInstanceId(instanceId);
                instanceMetaDataEntry.setPrivateId(privateId);
                instanceMetaDataEntry.setStartDate(clock.getCurrentTimeMillis());
                if (instanceMetaDataEntry.getClusterManagerServer() == null) {
                    instanceMetaDataEntry.setServer(Boolean.FALSE);
                }
                instanceMetaDataEntry.setLifeCycle(InstanceLifeCycle.fromCloudInstanceLifeCycle(md.getLifeCycle()));
                primaryIgSelected = setupInstanceMetaDataType(primaryIgSelected, terminatedPrimaryGwWhichShouldBeRestored, instanceMetaDataEntry, ig);
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
            primaryGWSelectionFallbackIfNecessary(primaryIgSelected, instanceGroups);

            return newInstances;
        } catch (CloudbreakImageNotFoundException | IllegalArgumentException ex) {
            throw new CloudbreakServiceException("Instance metadata collection failed", ex);
        }
    }

    private boolean setupInstanceMetaDataType(boolean primaryIgSelectedYet, Optional<InstanceMetaData> terminatedPrimaryGwWhichShouldBeRestored,
            InstanceMetaData instanceMetaDataEntry, InstanceGroup ig) {
        boolean primaryIgSelected = primaryIgSelectedYet;
        if (instanceMetaDataEntry.getInstanceMetadataType() == null) {
            if (ig != null) {
                LOGGER.info("Instance group type: {}", ig.getInstanceGroupType());
                if (InstanceGroupType.GATEWAY.equals(ig.getInstanceGroupType())) {
                    primaryIgSelected = setupGatewayInstance(primaryIgSelected, terminatedPrimaryGwWhichShouldBeRestored, instanceMetaDataEntry);
                } else {
                    LOGGER.info("Instance is a core instance: {}", instanceMetaDataEntry.getInstanceId());
                    instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
                }
            } else {
                LOGGER.info("Instance group is null, instance will be a core instance: {}", instanceMetaDataEntry.getInstanceId());
                instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.CORE);
            }
        }
        return primaryIgSelected;
    }

    private void setupFromCloudInstance(CloudInstance cloudInstance, InstanceMetaData instanceMetaDataEntry) {
        LOGGER.debug("Setup InstanceMetaData {}, from CloudInstance {}", instanceMetaDataEntry, cloudInstance);
        instanceMetaDataEntry.setInstanceName(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME));
    }

    private void setupFromCloudInstanceMetadata(CloudInstanceMetaData md, InstanceMetaData instanceMetaDataEntry) {
        LOGGER.debug("Setup InstanceMetaData {}, from CloudInstanceMetaData {}", instanceMetaDataEntry, md);
        instanceMetaDataEntry.setPrivateIp(md.getPrivateIp());
        instanceMetaDataEntry.setPublicIp(md.getPublicIp());
        instanceMetaDataEntry.setSshPort(md.getSshPort());
        instanceMetaDataEntry.setLocalityIndicator(md.getLocalityIndicator());
    }

    private void primaryGWSelectionFallbackIfNecessary(boolean primaryIgSelected, Map<String, InstanceGroup> instanceGroups) {
        if (!primaryIgSelected) {
            LOGGER.info("Primary GW was not selected! We will select one from the available GWs. It should not happen, it is a fallback mechanism");
            selectPrimaryGWFromGatewayInstances(instanceGroups.values());
        }
    }

    private boolean setupGatewayInstance(boolean primaryIgSelectedYet, Optional<InstanceMetaData> terminatedPrimaryGwWhichShouldBeRestored,
            InstanceMetaData instanceMetaDataEntry) {
        LOGGER.info("Instance is a gateway instance: {}", instanceMetaDataEntry.getInstanceId());
        if (terminatedPrimaryGwWhichShouldBeRestored.isPresent()) {
            LOGGER.info("Terminated primary GW should be restored: {}", terminatedPrimaryGwWhichShouldBeRestored.get());
            boolean primaryGWRestored = restorePrimaryGWIfFQDNMatch(terminatedPrimaryGwWhichShouldBeRestored.get(), instanceMetaDataEntry);
            if (primaryGWRestored) {
                return true;
            }
        } else {
            return selectPrimaryGWIfNotSelected(primaryIgSelectedYet, instanceMetaDataEntry);
        }
        return primaryIgSelectedYet;
    }

    private void selectPrimaryGWFromGatewayInstances(Collection<InstanceGroup> instanceGroups) {
        Optional<InstanceGroup> gwInstanceGroup = instanceGroups.stream()
                .filter(instanceGroup -> InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType()))
                .findFirst();

        if (gwInstanceGroup.isPresent()) {
            List<InstanceMetaData> gwInstances = instanceMetaDataService.findAllByInstanceGroupAndInstanceStatus(gwInstanceGroup.get(), InstanceStatus.CREATED);
            Optional<InstanceMetaData> gwInstance = gwInstances.stream().findFirst();
            if (gwInstance.isPresent()) {
                LOGGER.info("We were able to select primary GW from GW instances: {}", gwInstance);
                gwInstance.get().setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
                instanceMetaDataService.save(gwInstance.get());
            } else {
                LOGGER.error("We were not able to select primary gateway!");
                throw new CloudbreakServiceException("We were not able to select primary gateway!");
            }
        } else {
            LOGGER.error("There is no gateway group!");
            throw new CloudbreakServiceException("There is no gateway group!");
        }
    }

    private boolean selectPrimaryGWIfNotSelected(boolean primaryIgSelectedYet, InstanceMetaData instanceMetaDataEntry) {
        if (!primaryIgSelectedYet) {
            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
            instanceMetaDataEntry.setServer(Boolean.TRUE);
            LOGGER.info("Primary gateway is not selected, let's select this instance: {}", instanceMetaDataEntry.getInstanceId());
        } else {
            LOGGER.info("Primary gateway was selected. {} will be a normal gateway instance.", instanceMetaDataEntry.getInstanceId());
            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        }
        return true;
    }

    private boolean restorePrimaryGWIfFQDNMatch(InstanceMetaData terminatedPrimaryGwWhichShouldBeRestored, InstanceMetaData instanceMetaDataEntry) {
        if (terminatedPrimaryGwWhichShouldBeRestored.getDiscoveryFQDN().equals(instanceMetaDataEntry.getDiscoveryFQDN())) {
            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
            instanceMetaDataEntry.setServer(Boolean.TRUE);
            LOGGER.info("Instance will be a primary GW: {}", instanceMetaDataEntry);
            return true;
        } else {
            LOGGER.info("Primary gateway is another instance not this one: {}", instanceMetaDataEntry.getInstanceId());
            instanceMetaDataEntry.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
            return false;
        }
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
                loadBalancerEntry.setProviderConfig(loadBalancerConfigConverter.convertLoadBalancer(stack.getCloudPlatform(),
                    cloudLoadBalancerMetadata));

                loadBalancerPersistenceService.save(loadBalancerEntry);

                Set<TargetGroup> targetGroups = targetGroupPersistenceService.findByLoadBalancerId(loadBalancerEntry.getId());
                for (TargetGroup targetGroup : targetGroups) {
                    targetGroup.setProviderConfig(loadBalancerConfigConverter.convertTargetGroup(stack.getCloudPlatform(),
                        cloudLoadBalancerMetadata, targetGroup));
                    targetGroupPersistenceService.save(targetGroup);
                }
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
