package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClouderaManagerPollingUtilService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class DeleteVolumesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ClouderaManagerPollingUtilService clouderaManagerPollingUtilService;

    @Inject
    private TemplateService templateService;

    @Inject
    private InstanceGroupService instanceGroupService;

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
                && resource.getInstanceGroup().equals(stackDeleteVolumesRequest.getGroup()) && resource.getResourceType()
                .equals(ResourceType.AWS_VOLUMESET)).collect(Collectors.toList());
        LOGGER.debug("Deleting resources from CBDB {}", resourcesToBeDeleted);
        resourceService.deleteAll(resourcesToBeDeleted);
        updateTemplate(stackDto.getId(), stackDeleteVolumesRequest.getGroup());
    }

    public void stopClouderaManagerService(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents) throws Exception {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Stopping CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().stopClouderaManagerService(serviceComponent.getService());
                clouderaManagerPollingUtilService.pollClouderaManagerServices(clusterApi, serviceComponent.getService(), "STOPPED");
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
                clouderaManagerPollingUtilService.pollClouderaManagerServices(clusterApi, serviceComponent.getService(), "STARTED");
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
            for (VolumeTemplate volumeTemplateInTheDatabase : template.getVolumeTemplates()) {
                volumeTemplateInTheDatabase.setVolumeCount(0);
            }
            templateService.savePure(template);
        }
    }
}
