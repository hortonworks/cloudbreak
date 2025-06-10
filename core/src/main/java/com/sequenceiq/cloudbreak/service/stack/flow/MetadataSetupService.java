package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INSTANCE_METADATA_RESTORED;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.projection.StackIdView;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigConverter;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class MetadataSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataSetupService.class);

    private static final String ENDPOINT_SUFFIX = "gateway";

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
    private StackService stackService;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private Clock clock;

    @Inject
    private TransactionService transactionService;

    @Inject
    private LoadBalancerConfigConverter loadBalancerConfigConverter;

    @Inject
    private CloudbreakEventService eventService;

    public void handleRepairFail(Long stackId, Set<String> hostNames) {
        LOGGER.info("Handle repair fail for hostnames: {}", hostNames);
        for (String hostName : hostNames) {
            try {
                transactionService.required(() -> {
                    Optional<InstanceMetaData> instance = instanceMetaDataService.findByHostname(stackId, hostName);
                    instance.ifPresentOrElse(instanceMetaData -> {
                        LOGGER.info("Instance status for {}: {}", hostName, instanceMetaData.getInstanceStatus());
                        if (InstanceStatus.REQUESTED.equals(instanceMetaData.getInstanceStatus())) {
                            LOGGER.info("Instance in requested status is deleted: {}", hostName);
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
        LOGGER.info("Get last terminated instance for this hostname: {}", hostName);
        Optional<InstanceMetaData> lastTerminatedInstanceMetadataWithInstanceIdByFQDN =
                instanceMetaDataService.getTerminatedInstanceMetadataWithInstanceIdByFQDNOrdered(stackId, hostName);
        LOGGER.info("Restore previous terminated instance for hostname: {}", lastTerminatedInstanceMetadataWithInstanceIdByFQDN);
        lastTerminatedInstanceMetadataWithInstanceIdByFQDN.ifPresent(instanceMetaData -> {
            instanceMetaData.setTerminationDate(null);
            instanceMetaData.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
            instanceMetaDataService.save(instanceMetaData);
            eventService.fireCloudbreakEvent(stackId, UPDATE_FAILED.name(), STACK_INSTANCE_METADATA_RESTORED,
                    List.of(hostName));
        });
    }

    public void cleanupRequestedInstancesIfNotInList(Long stackId, Set<String> instanceGroups, Set<Long> privateIds) {
        LOGGER.info("Cleanup the requested instances if private id not in {}", privateIds);
        for (String instanceGroupName : instanceGroups) {
            Optional<InstanceGroup> instanceGroup = instanceGroupService.findOneByStackIdAndGroupName(stackId, instanceGroupName);
            instanceGroup.ifPresent(ig -> {
                List<InstanceMetaData> requestedInstanceMetaDatas =
                        instanceMetaDataService.findAllByInstanceGroupAndInstanceStatus(ig, InstanceStatus.REQUESTED);
                LOGGER.info("Instances in requested state: {}", requestedInstanceMetaDatas);
                List<InstanceMetaData> removableInstanceMetaDatas = requestedInstanceMetaDatas.stream()
                        .filter(instanceMetaData -> !privateIds.contains(instanceMetaData.getPrivateId()))
                        .collect(Collectors.toList());
                LOGGER.info("Cleanup the following instances: {}", requestedInstanceMetaDatas);
                for (InstanceMetaData removableInstanceMetaData : removableInstanceMetaDatas) {
                    removableInstanceMetaData.setTerminationDate(clock.getCurrentTimeMillis());
                    removableInstanceMetaData.setInstanceStatus(InstanceStatus.TERMINATED);
                }
                instanceMetaDataService.saveAll(removableInstanceMetaDatas);
            });
        }
    }

    public void cleanupRequestedInstancesWithoutFQDN(Long stackId, Set<String> instanceGroups) {
        try {
            transactionService.required(() -> {
                for (String instanceGroupName : instanceGroups) {
                    Optional<InstanceGroup> ig = instanceGroupService.findOneWithInstanceMetadataByGroupNameInStack(stackId, instanceGroupName);
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
                }
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public int saveInstanceMetaData(StackView stack, Iterable<CloudVmMetaDataStatus> cloudVmMetaDataStatusList, InstanceStatus status) {
        try {
            LOGGER.info("Save instance metadata for stack: {}", stack.getName());
            int newInstances = 0;
            Set<InstanceMetaData> allInstanceMetadata = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
            boolean primaryIgSelected = allInstanceMetadata.stream().anyMatch(imd -> imd.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY);

            Map<String, InstanceGroup> instanceGroups = instanceGroupService.getByStackAndFetchTemplates(stack.getId())
                    .stream()
                    .collect(Collectors.toMap(InstanceGroup::getGroupName, Function.identity()));

            Optional<InstanceMetaData> terminatedPrimaryGwWhichShouldBeRestored = Optional.empty();
            if (!primaryIgSelected) {
                terminatedPrimaryGwWhichShouldBeRestored = instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(stack.getId());
                LOGGER.info("Terminated primary GW which should be restored: {}", terminatedPrimaryGwWhichShouldBeRestored);
            }

            for (CloudVmMetaDataStatus cloudVmMetaDataStatus : cloudVmMetaDataStatusList) {
                if (cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == CREATED ||
                        cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == STARTED) {
                    newInstances++;
                }
                primaryIgSelected = saveInstanceMetaData(stack, status, cloudVmMetaDataStatus, allInstanceMetadata, instanceGroups,
                        primaryIgSelected, terminatedPrimaryGwWhichShouldBeRestored);
            }
            primaryGWSelectionFallbackIfNecessary(primaryIgSelected, instanceGroups);

            return newInstances;
        } catch (CloudbreakImageNotFoundException | IllegalArgumentException ex) {
            throw new CloudbreakServiceException("Instance metadata collection failed", ex);
        }
    }

    private boolean saveInstanceMetaData(StackView stack, InstanceStatus status, CloudVmMetaDataStatus cloudVmMetaDataStatus,
            Set<InstanceMetaData> allInstanceMetadata, Map<String, InstanceGroup> instanceGroups, boolean primaryIgSelected,
            Optional<InstanceMetaData> terminatedPrimaryGwWhichShouldBeRestored) throws CloudbreakImageNotFoundException {
        CloudInstance cloudInstance = cloudVmMetaDataStatus.getCloudVmInstanceStatus().getCloudInstance();
        CloudInstanceMetaData md = cloudVmMetaDataStatus.getMetaData();
        InstanceTemplate template = cloudInstance.getTemplate();
        Long privateId = template.getPrivateId();
        String instanceId = cloudInstance.getInstanceId();
        InstanceMetaData instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId);
        // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
        InstanceGroup ig = instanceMetaDataEntry.getInstanceGroup();
        String group = ig == null ? template.getGroupName() : ig.getGroupName();
        InstanceGroup instanceGroup = instanceGroups.get(group);
        setupFromCloudInstanceMetadata(md, instanceMetaDataEntry);
        setupFromCloudInstance(cloudInstance, instanceMetaDataEntry);
        instanceMetaDataEntry.setInstanceGroup(instanceGroup);
        if (!StringUtils.hasText(instanceMetaDataEntry.getProviderInstanceType())) {
            instanceMetaDataEntry.setProviderInstanceType(instanceGroup.getTemplate() != null ? instanceGroup.getTemplate().getInstanceType() : null);
        }
        instanceMetaDataEntry.setInstanceId(instanceId);
        instanceMetaDataEntry.setPrivateId(privateId);
        instanceMetaDataEntry.setStartDate(clock.getCurrentTimeMillis());
        if (instanceMetaDataEntry.getClusterManagerServer() == null) {
            instanceMetaDataEntry.setServer(Boolean.FALSE);
        }
        instanceMetaDataEntry.setLifeCycle(InstanceLifeCycle.fromCloudInstanceLifeCycle(md.getLifeCycle()));
        primaryIgSelected = setupInstanceMetaDataType(primaryIgSelected, terminatedPrimaryGwWhichShouldBeRestored, instanceMetaDataEntry, ig);
        if (status != null && instanceMetaDataEntry.getInstanceStatus() != InstanceStatus.ZOMBIE) {
            if (cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus() == TERMINATED) {
                instanceMetaDataEntry.setInstanceStatus(InstanceStatus.TERMINATED);
            } else {
                instanceMetaDataEntry.setInstanceStatus(status);
                if (instanceMetaDataEntry.getImage() == null || !StringUtils.hasText(instanceMetaDataEntry.getImage().getValue())) {
                    Image image = imageService.getImage(stack.getId());
                    LOGGER.debug("Add image {} for instance metadata: {}", image.getImageId(), instanceMetaDataEntry);
                    instanceMetaDataEntry.setImage(new Json(image));
                }
            }
        }
        instanceMetaDataService.save(instanceMetaDataEntry);
        return primaryIgSelected;
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
            // Ordered list of metadata to guarantee consistent primary gateway generation across multiple cluster recoveries
            List<InstanceMetaData> gwInstances = instanceMetaDataService.findAllByInstanceGroupAndInstanceStatusOrdered(
                    gwInstanceGroup.get(), InstanceStatus.CREATED);
            LOGGER.debug("Found Gateway instances {}", gwInstances);
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
        if (terminatedPrimaryGwWhichShouldBeRestored.getDiscoveryFQDN() != null
                && terminatedPrimaryGwWhichShouldBeRestored.getDiscoveryFQDN().equals(instanceMetaDataEntry.getDiscoveryFQDN())) {
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

    public void saveLoadBalancerMetadata(StackView stack, Iterable<CloudLoadBalancerMetadata> cloudLoadBalancerMetadataList) {
        try {
            LOGGER.info("Save load balancer metadata for stack: {}", stack.getName());

            validateCloudLoadBalancerMetadata(cloudLoadBalancerMetadataList);

            Set<LoadBalancer> allLoadBalancerMetadata = loadBalancerPersistenceService.findByStackId(stack.getId());

            for (CloudLoadBalancerMetadata cloudLoadBalancerMetadata : cloudLoadBalancerMetadataList) {
                LoadBalancer loadBalancerEntry = createLoadBalancerMetadataIfAbsent(allLoadBalancerMetadata,
                        stack, cloudLoadBalancerMetadata.getType());

                loadBalancerEntry.setDns(cloudLoadBalancerMetadata.getCloudDns());
                loadBalancerEntry.setHostedZoneId(cloudLoadBalancerMetadata.getHostedZoneId());
                loadBalancerEntry.setIp(cloudLoadBalancerMetadata.getIp());
                loadBalancerEntry.setType(cloudLoadBalancerMetadata.getType());
                String endpoint = generateLoadBalancerEndpoint(stack);

                List<StackIdView> byEnvironmentCrnAndStackType = stackService.getByEnvironmentCrnAndStackType(stack.getEnvironmentCrn(), StackType.DATALAKE);
                List<StackStatus> stoppedDatalakes = byEnvironmentCrnAndStackType
                        .stream().map(s -> stackStatusService.findFirstByStackIdOrderByCreatedDesc(s.getId()))
                        .filter(Optional::isPresent).map(Optional::get)
                        .filter(status -> status.getStatus().isStopState()).collect(Collectors.toList());


                if (!stoppedDatalakes.isEmpty()) {
                    /* Starts to check for a situation where we are resizing a datalake that did not previously have loadbalancers
                        so that we can use the same endpoint name for a seamless transition
                     */
                    LOGGER.info("Using old datalake endpoint name for resized datalake: {}, env: {}", stack.getName(), stack.getEnvironmentCrn());
                    if (stoppedDatalakes.size() > 1) {
                        String ids = stoppedDatalakes.stream().map(stackStatus -> stackStatus.getStack().getId())
                                .map(Object::toString).collect(Collectors.joining(","));
                        LOGGER.warn("more than one datalake found to resize from: {}", ids);
                    }
                    Long oldId = stoppedDatalakes.getFirst().getStack().getId();
                    Set<LoadBalancer> oldLoadBalancers = loadBalancerPersistenceService.findByStackId(oldId);
                    if (oldLoadBalancers.isEmpty()) {
                        Stack oldStack = stackService.getByIdWithGatewayInTransaction(oldId);
                        if (stack.getDisplayName().equals(oldStack.getDisplayName())) {
                            endpoint = oldStack.getPrimaryGatewayInstance().getShortHostname();
                        }
                    }
                }

                LOGGER.info("Saving load balancer endpoint as: {}", endpoint);
                loadBalancerEntry.setEndpoint(endpoint);
                loadBalancerEntry.setProviderConfig(loadBalancerConfigConverter.convertLoadBalancer(stack.getCloudPlatform(),
                    cloudLoadBalancerMetadata, loadBalancerEntry.getSku()));

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

    private void validateCloudLoadBalancerMetadata(Iterable<CloudLoadBalancerMetadata> loadBalancers) {
        Set<CloudLoadBalancerMetadata> loadbalancersMissingMetadata =
                StreamSupport
                        .stream(loadBalancers.spliterator(), false)
                        .filter(this::isMissingMetadata)
                        .collect(Collectors.toSet());
        if (!loadbalancersMissingMetadata.isEmpty()) {
            LOGGER.error("Load Balancers missing metadata: {}", loadbalancersMissingMetadata);
            Set<String> names =
                    loadbalancersMissingMetadata
                            .stream()
                            .map(CloudLoadBalancerMetadata::getName)
                            .collect(Collectors.toSet());
            throw new CloudbreakServiceException("Creation failed for load balancers: " + names);
        }
    }

    private boolean isMissingMetadata(CloudLoadBalancerMetadata cloudLoadBalancerMetadata) {
        return cloudLoadBalancerMetadata.getType() == null ||
                !(StringUtils.hasText(cloudLoadBalancerMetadata.getIp()) ||
                        StringUtils.hasText(cloudLoadBalancerMetadata.getCloudDns())
                                && StringUtils.hasText(cloudLoadBalancerMetadata.getHostedZoneId()));
    }

    private LoadBalancer createLoadBalancerMetadataIfAbsent(Iterable<LoadBalancer> allLoadBalancerMetadata,
            StackView stack, LoadBalancerType type) {
        if (stack != null && type != null) {
            for (LoadBalancer loadBalancerMetadata : allLoadBalancerMetadata) {
                if (Objects.equals(stack.getId(), loadBalancerMetadata.getStackId()) &&
                        type == loadBalancerMetadata.getType()) {
                    return loadBalancerMetadata;
                }
            }
        }
        return new LoadBalancer();
    }

    private String generateLoadBalancerEndpoint(StackView stack) {
        return stack.getName() +
                '-' +
                ENDPOINT_SUFFIX;
    }

}