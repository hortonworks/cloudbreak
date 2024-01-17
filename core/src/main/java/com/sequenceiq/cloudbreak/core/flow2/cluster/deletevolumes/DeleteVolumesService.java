package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class DeleteVolumesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesService.class);

    private static final long SLEEP_INTERVAL = 15L;

    private static final long TIMEOUT_DURATION = 3L;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private TemplateService templateService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackService stackService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    public void detachResources(List<CloudResource> cloudResourcesToBeDetached, CloudPlatformVariant cloudPlatformVariant, AuthenticatedContext ac)
            throws Exception {
        LOGGER.debug("Detaching volumes {}", cloudResourcesToBeDetached);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        cloudConnector.volumeConnector().detachVolumes(ac, cloudResourcesToBeDetached);
    }

    public void deleteResources(List<CloudResource> cloudResourcesToBeDeleted, CloudPlatformVariant cloudPlatformVariant, AuthenticatedContext ac)
            throws Exception {
        LOGGER.debug("Deleting volumes {}", cloudResourcesToBeDeleted);
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        cloudConnector.volumeConnector().deleteVolumes(ac, cloudResourcesToBeDeleted);
    }

    public void deleteVolumeResources(StackDto stackDto, DeleteVolumesHandlerRequest payload) throws Exception {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = payload.getStackDeleteVolumesRequest();
        List<Resource> resourcesToBeDeleted = stackDto.getResources().stream().filter(resource -> null != resource.getInstanceGroup()
                && resource.getInstanceGroup().equals(stackDeleteVolumesRequest.getGroup()) && resource.getResourceType().toString()
                .contains("VOLUMESET")).collect(Collectors.toList());
        LOGGER.debug("Deleting volumeset attributes of resources from CBDB. Updated resources: {}", resourcesToBeDeleted);
        if (!resourcesToBeDeleted.isEmpty()) {
            resourceService.saveAll(resourcesToBeDeleted.stream()
                .peek(volumeSet -> resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                    volumeSetAttributes.setVolumes(List.of());
                    resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
                }))
                .collect(Collectors.toList()));
        }
        updateTemplate(stackDto.getId(), stackDeleteVolumesRequest.getGroup());
    }

    public void stopClouderaManagerService(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents) throws Exception {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Stopping CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().stopClouderaManagerService(serviceComponent.getService());
            } catch (Exception e) {
                LOGGER.error("Unable to stop CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakException(String.format("Unable to stop CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    public void startClouderaManagerService(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents) throws Exception {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Starting CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().startClouderaManagerService(serviceComponent.getService());
            } catch (Exception e) {
                LOGGER.error("Unable to start CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakException(String.format("Unable to start CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    private void updateTemplate(Long stackId, String group) {
        LOGGER.debug("Updating stack template and saving it to CBDB.");
        Optional<InstanceGroupView> optionalGroup = instanceGroupService
                .findInstanceGroupViewByStackIdAndGroupName(stackId, group);
        if (optionalGroup.isPresent()) {
            InstanceGroupView instanceGroup = optionalGroup.get();
            Template template = templateService.get(instanceGroup.getTemplate().getId());
            template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES_ONLY);
            for (VolumeTemplate volumeTemplateInTheDatabase : template.getVolumeTemplates()) {
                volumeTemplateInTheDatabase.setVolumeCount(0);
            }
            templateService.savePure(template);
        }
    }

    public void unmountBlockStorageDisks(Stack stack, String requestGroup) throws Exception {
        String blueprintText = stack.getBlueprint().getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
        StackDto stackDto = stackDtoService.getById(stack.getId());
        stopClouderaManagerService(stackDto, hostTemplateServiceComponents);
        Set<Node> allNodes = stackUtil.collectNodes(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup)).collect(Collectors.toSet());
        InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
        LOGGER.info("RE-Bootstrap machines - This is required for pushing script to nodes.");
        clusterBootstrapper.reBootstrapMachines(stack.getId());
        Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup))
                .collect(Collectors.toSet());
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId());
        LOGGER.debug("Unmounting all attached block storages to detach and delete.");
        hostOrchestrator.unmountBlockStorageDisks(gatewayConfigs, nodesWithDiskData, allNodes, exitCriteriaModel);
    }

    public void updateScriptsAndRebootInstances(Long stackId, String requestGroup) {
        StackDto stackDto = stackDtoService.getById(stackId);
        LOGGER.debug("This updates salt properties on the scripts and distributes scripts to all nodes.");
        clusterHostServiceRunner.updateClusterConfigs(stackDto, true);
        Stack stack = stackService.getByIdWithLists(stackId);
        List<Resource> resourceList = resourceService.findAllByStackIdAndResourceTypeIn(stackId,
                List.of(stack.getDiskResourceType())).stream().filter(res -> null != res.getInstanceId()).toList();
        stack.setResources(new HashSet<>(resourceList));
        List<InstanceMetadataView> instanceMetadataViews = stack.getInstanceGroupDtos().stream().filter(ig -> !ig.getNotDeletedInstanceMetaData().isEmpty()
                && ig.getInstanceGroup().getGroupName().equals(requestGroup)).map(InstanceGroupDto::getNotDeletedInstanceMetaData).flatMap(Collection::stream)
                .toList();
        List<CloudInstance> cloudInstances = instanceMetaDataToCloudInstanceConverter.convert(instanceMetadataViews, stack);
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(stack.getCloudPlatform()),
                Variant.variant(stack.getPlatformVariant()));
        CloudConnector cloudConnector = cloudPlatformConnectors.get(cloudPlatformVariant);
        CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
        CloudContext cloudContext = getCloudContext(stack);
        AuthenticatedContext ac = cloudConnector.authentication().authenticate(cloudContext, cloudCredential);
        LOGGER.debug("Rebooting cloud instances to run mount scripts.");
        cloudConnector.instances().reboot(ac, null, cloudInstances);
        getFstabAndPersist(stack, requestGroup);
    }

    private CloudContext getCloudContext(Stack stack) {
        Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));
        return CloudContext.Builder.builder()
            .withId(stack.getId())
            .withName(stack.getName())
            .withCrn(stack.getResourceCrn())
            .withPlatform(stack.getCloudPlatform())
            .withVariant(stack.getPlatformVariant())
            .withLocation(location)
            .withWorkspaceId(stack.getWorkspace().getId())
            .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
            .build();
    }

    private void getFstabAndPersist(Stack stack, String requestGroup) {
        Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup))
                .collect(Collectors.toSet());
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<String> requestGroupHostnames = nodesWithDiskData.stream().map(Node::getHostname)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        LOGGER.debug("Getting fstab information. Using poller so that it will wait for the mount scripts to finish.");
        List<Map<String, String>> fstabResponse = Lists.newArrayList();
        pollingFstab(gatewayConfigs, requestGroupHostnames, fstabResponse);
        InMemoryStateStore.deleteStack(stack.getId());
        Map<String, Map<String, String>> fstabInformation = nodesWithDiskData.stream()
            .map(node -> {
                String fstab = fstabResponse.get(0).getOrDefault(node.getHostname(), "");
                return new AbstractMap.SimpleImmutableEntry<>(node.getHostname(), Map.of("uuids", "", "fstab", fstab));
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        parseFstabAndPersistDiskInformation(fstabInformation, stack);
    }

    private void pollingFstab(List<GatewayConfig> gatewayConfigs, Set<String> requestGroupHostnames, List<Map<String, String>> fstabResponse) {
        PollingConfig pollingConfig = PollingConfig.builder().withSleepTime(SLEEP_INTERVAL)
                .withSleepTimeUnit(TimeUnit.SECONDS).withTimeout(TIMEOUT_DURATION).withTimeoutTimeUnit(TimeUnit.MINUTES).build();
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(false)
                .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .run(() -> getAttemptResult(gatewayConfigs, requestGroupHostnames, fstabResponse));
    }

    private AttemptResult<Void> getAttemptResult(List<GatewayConfig> gatewayConfigs, Set<String> requestGroupHostnames,
            List<Map<String, String>> fstabResponse) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Getting instance fstabs for hostnames - {}", requestGroupHostnames);
        Map<String, String> response = hostOrchestrator.runCommandOnHosts(gatewayConfigs, requestGroupHostnames, "cat /etc/fstab");
        boolean mountComplete = response.values().stream().filter(str -> str.isEmpty() || "false".equals(str)).toList().size() == 0;
        if (mountComplete) {
            LOGGER.debug("Returned instance fstabs - {}", response);
            fstabResponse.add(response);
            return AttemptResults.justFinish();
        }
        return AttemptResults.justContinue();
    }

    private void parseFstabAndPersistDiskInformation(Map<String, Map<String, String>> fstabInformation, Stack stack) {
        LOGGER.debug("Parsing fstab information from host orchestrator mounting additional volumes - {}", fstabInformation);
        fstabInformation.forEach((hostname, value) -> {
            Optional<String> instanceIdOptional = stack.getInstanceMetaDataAsList().stream()
                    .filter(instanceMetaData -> hostname.equals(instanceMetaData.getDiscoveryFQDN()))
                    .map(InstanceMetaData::getInstanceId)
                    .findFirst();

            if (instanceIdOptional.isPresent()) {
                String uuids = value.getOrDefault("uuids", "");
                String fstab = value.getOrDefault("fstab", "");
                if (!StringUtils.isEmpty(fstab)) {
                    LOGGER.debug("Persisting resources for instance id - {}, hostname - {}, uuids - {}, fstab - {}.", instanceIdOptional.get(), hostname,
                            uuids, fstab);
                    persistUuidAndFstab(stack, instanceIdOptional.get(), hostname, uuids, fstab);
                }
            }
        });
    }

    private void persistUuidAndFstab(Stack stack, String instanceId, String discoveryFQDN, String uuids, String fstab) {
        resourceService.saveAll(stack.getDiskResources().stream()
                .filter(volumeSet -> instanceId.equals(volumeSet.getInstanceId()))
                .peek(volumeSet -> resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                    volumeSetAttributes.setUuids(uuids);
                    volumeSetAttributes.setFstab(fstab);
                    if (!discoveryFQDN.equals(volumeSetAttributes.getDiscoveryFQDN())) {
                        LOGGER.info("DiscoveryFQDN is updated for {} to {}", volumeSet.getResourceName(), discoveryFQDN);
                    }
                    volumeSetAttributes.setDiscoveryFQDN(discoveryFQDN);
                    resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
                }))
                .collect(Collectors.toList()));
    }
}
