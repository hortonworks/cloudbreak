package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VERTICALSCALING;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.InstanceStorageInfo;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class CoreVerticalScaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreVerticalScaleService.class);

    private static final int MAX_READ_COUNT = 15;

    private static final int SLEEP_INTERVAL = 10;

    private static final String YARN_LOCAL_DIR = "yarn_nodemanager_local_dirs";

    private static final String YARN_LOG_DIR = "yarn_nodemanager_log_dirs";

    private static final String IMPALA_SCRATCH_DIR = "scratch_dirs";

    private static final String IMPALA_DATACACHE_DIR = "datacache_dirs";

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TemplateService templateService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    public void verticalScale(Long stackId, StackVerticalScaleV4Request payload) {
        LOGGER.debug("Updating stack status to Update in Progress.");
        flowMessageService.fireEventAndLog(stackId,
                Status.UPDATE_IN_PROGRESS.name(),
                CLUSTER_VERTICALSCALING,
                payload.getGroup(),
                payload.getTemplate().getInstanceType());
    }

    public void finishVerticalScale(Long stackId, StackVerticalScaleV4Request payload, boolean stackStopped) {
        DetailedStackStatus statusToUpdate = stackStopped ? DetailedStackStatus.STOPPED : DetailedStackStatus.AVAILABLE;
        clusterService.updateClusterStatusByStackId(stackId, statusToUpdate);
        flowMessageService.fireEventAndLog(stackId,
                Status.STOPPED.name(),
                CLUSTER_VERTICALSCALED,
                payload.getGroup(),
                payload.getTemplate().getInstanceType());
    }

    public void updateTemplateWithVerticalScaleInformation(Long stackId, StackVerticalScaleV4Request stackVerticalScaleV4Request,
            List<InstanceStorageInfo> instanceStoreInfo) {
        LOGGER.debug("Updating stack template and saving it to CBDB.");
        Optional<InstanceGroupView> optionalGroup = instanceGroupService
                .findInstanceGroupViewByStackIdAndGroupName(stackId, stackVerticalScaleV4Request.getGroup());
        if (optionalGroup.isPresent() && stackVerticalScaleV4Request.getTemplate() != null) {
            InstanceGroupView group = optionalGroup.get();
            Template template = templateService.get(group.getTemplate().getId());
            InstanceTemplateV4Request requestedTemplate = stackVerticalScaleV4Request.getTemplate();
            String instanceType = requestedTemplate.getInstanceType();
            if (!Strings.isNullOrEmpty(instanceType)) {
                template.setInstanceType(instanceType);
            }
            if (requestedTemplate.getRootVolume() != null) {
                Integer rootVolumeSize = requestedTemplate.getRootVolume().getSize();
                template.setRootVolumeSize(rootVolumeSize);
            }
            setTemporaryStorageOnTemplate(template, instanceStoreInfo);
            Set<VolumeV4Request> requestedAttachedVolumes = requestedTemplate.getAttachedVolumes();
            if (requestedAttachedVolumes != null) {
                for (VolumeTemplate volumeTemplateInTheDatabase : template.getVolumeTemplates()) {
                    for (VolumeV4Request volumeV4Request : requestedAttachedVolumes) {
                        if (volumeTemplateInTheDatabase.getVolumeType().equals(volumeV4Request.getType())) {
                            volumeTemplateInTheDatabase.setVolumeCount(volumeV4Request.getSize());
                            volumeTemplateInTheDatabase.setVolumeSize(volumeV4Request.getCount());
                        }
                    }
                }
            }
            templateService.savePure(template);
        }
    }

    public void stopClouderaManagerServicesAndUpdateClusterConfigs(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents,
            List<InstanceStorageInfo> instanceStorageInfo) throws Exception {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Stopping CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().stopClouderaManagerService(serviceComponent.getService());
                pollClouderaManagerServices(clusterApi, serviceComponent.getService(), "STOPPED");
            } catch (Exception e) {
                LOGGER.error("Unable to stop CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new Exception(String.format("Unable to stop CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
        InMemoryStateStore.putStack(stackDto.getId(), PollGroup.POLLABLE);
        LOGGER.debug("Redeploying states to the gateway node.");
        clusterHostServiceRunner.redeployStates(stackDto);
        LOGGER.debug("Updating salt, pillar configs for the stack and pushing it to the group nodes.");
        clusterHostServiceRunner.updateClusterConfigs(stackDto, true);
        InMemoryStateStore.deleteStack(stackDto.getId());
    }

    public void updateClouderaManagerConfigsForComputeGroupAndStartServices(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents,
            List<InstanceStorageInfo> instanceStorageInfo, List<String> roleGroupNames) throws Exception {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Updating CM service config for service {}, in stack {} for roles {}", serviceComponent.getService(),
                        stackDto.getId(), roleGroupNames);
                clusterApi.clusterModificationService().updateServiceConfig(serviceComponent.getService(),
                    getConfigsForService(instanceStorageInfo, stackDto, serviceComponent.getService()), roleGroupNames);
                LOGGER.debug("Starting CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().startClouderaManagerService(serviceComponent.getService());
                pollClouderaManagerServices(clusterApi, serviceComponent.getService(), "STARTED");
            } catch (Exception e) {
                LOGGER.error("Unable to start CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new Exception(String.format("Unable to start CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    public void stopInstances(CloudConnector connector, List<CloudResource> resources, InstanceGroupDto instanceGroup,
            StackDto stackDto, AuthenticatedContext ac) {
        List<String> instancesToBeStopped = instanceGroup.getInstanceMetadataViews().stream()
                .map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesToBeStopped = instanceMetaDataToCloudInstanceConverter.convert(List.of(instanceGroup),
                stackDto.getEnvironmentCrn(), stackDto.getStackAuthentication());
        List<CloudResource> resourceToBeStopped = resources.stream()
                .filter(cr -> instancesToBeStopped.contains(cr.getInstanceId()))
                .collect(Collectors.toList());
        LOGGER.debug("Stopping cloud resources {}, in stack {}", resources, stackDto.getId());
        connector.instances().stop(ac, resourceToBeStopped, cloudInstancesToBeStopped);
    }

    public void startInstances(CloudConnector connector, List<CloudResource> resources, InstanceGroupDto instanceGroup,
            StackDto stackDto, AuthenticatedContext ac) {
        List<String> instancesToBeStarted = instanceGroup.getInstanceMetadataViews().stream()
                .map(InstanceMetadataView::getInstanceId).collect(Collectors.toList());
        List<CloudInstance> cloudInstancesToBeStarted = instanceMetaDataToCloudInstanceConverter.convert(List.of(instanceGroup),
                stackDto.getEnvironmentCrn(), stackDto.getStackAuthentication());
        List<CloudResource> resourceToBeStarted = resources.stream()
                .filter(cr -> instancesToBeStarted.contains(cr.getInstanceId()))
                .collect(Collectors.toList());
        LOGGER.debug("Starting cloud resources {}, in stack {}", resources, stackDto.getId());
        connector.instances().start(ac, resourceToBeStarted, cloudInstancesToBeStarted);
    }

    private Map<String, String> getConfigsForService(List<InstanceStorageInfo> instanceStorageInfo,
            StackDto stackDto, String service) {
        LOGGER.debug("Building configs to be updated for service {} in CM", service);
        Map<String, String> config = new HashMap<>();
        StringBuilder localMountPaths = new StringBuilder();
        StringBuilder logMountPaths = new StringBuilder();
        if (instanceStorageInfo != null && !instanceStorageInfo.isEmpty()) {
            int instanceStorageCount = instanceStorageInfo.get(0).getInstanceStorageCount();
            int count = 1;
            while (instanceStorageCount > 0) {
                if ("YARN".equalsIgnoreCase(service)) {
                    localMountPaths.append("/hadoopfs/ephfs").append(count).append("/nodemanager");
                    logMountPaths.append("/hadoopfs/ephfs").append(count).append("/nodemanager/log");
                } else if ("IMPALA".equalsIgnoreCase(service)) {
                    localMountPaths.append("/hadoopfs/ephfs").append(count).append("/impala/scratch");
                    logMountPaths.append("/hadoopfs/ephfs").append(count).append("/impala/datacache");
                }
                if (instanceStorageCount - 1 > 0) {
                    localMountPaths.append(',');
                    logMountPaths.append(',');
                }
                count++;
                instanceStorageCount--;
            }
        }
        if ("YARN".equalsIgnoreCase(service)) {
            config.put(YARN_LOCAL_DIR, localMountPaths.toString());
            config.put(YARN_LOG_DIR, logMountPaths.toString());
        } else if ("IMPALA".equalsIgnoreCase(service)) {
            config.put(IMPALA_SCRATCH_DIR, localMountPaths.toString());
            config.put(IMPALA_DATACACHE_DIR, logMountPaths.toString());
        }
        LOGGER.debug("Configs {} to be updated for service {} in CM", config, service);
        return config;
    }

    private void setTemporaryStorageOnTemplate(Template template, List<InstanceStorageInfo> instanceStoreInfo) {
        LOGGER.debug("Setting temporary storage on stack and attached number of disks.");
        if (instanceStoreInfo != null && !instanceStoreInfo.isEmpty()) {
            template.setTemporaryStorage(TemporaryStorage.EPHEMERAL_VOLUMES);
            template.setInstanceStorageCount(instanceStoreInfo.get(0).getInstanceStorageCount());
            template.setInstanceStorageSize(instanceStoreInfo.get(0).getInstanceStorageSize());
        } else {
            template.setTemporaryStorage(TemporaryStorage.ATTACHED_VOLUMES);
            template.setInstanceStorageCount(0);
            template.setInstanceStorageSize(0);
        }
    }

    private void pollClouderaManagerServices(ClusterApi clusterApi, String service, String status) throws Exception {
        LOGGER.debug("Starting polling on CM Service {} to check if {}", service, status);
        Polling.waitPeriodly(SLEEP_INTERVAL, TimeUnit.SECONDS).stopIfException(true).stopAfterAttempt(MAX_READ_COUNT)
            .run(() -> {
                LOGGER.debug("Polling CM Service {} to check if {}", service, status);
                Map<String, String> readResults = clusterApi.clusterModificationService().fetchServiceStatuses();
                if (status.equals(readResults.get(service.toLowerCase()))) {
                    return AttemptResults.justFinish();
                }
                return AttemptResults.justContinue();
            });
    }
}
