package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmHostGroupRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class ConfigUpdateUtilService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUpdateUtilService.class);

    private static final int ZERO = 0;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private List<CmHostGroupRoleConfigProvider> cmHostGroupRoleConfigProviders;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    public void updateCMConfigsForComputeAndStartServices(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents,
            List<String> roleGroupNames, String requestGroup) throws CloudbreakServiceException {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Updating CM service config for service {}, in stack {} for roles {}", serviceComponent.getService(),
                        stackDto.getId(), roleGroupNames);
                Map<String, String> configMap = getConfigsForService(serviceComponent, stackDto, requestGroup);
                if (!configMap.isEmpty()) {
                    String roleGroup = serviceComponent.getService().toLowerCase(Locale.ROOT) + "-" + serviceComponent.getComponent();
                    List<String> serviceRoleGroup = roleGroupNames.stream().filter(s -> s.startsWith(roleGroup)).toList();
                    clusterApi.clusterModificationService().updateServiceConfig(serviceComponent.getService(), configMap, serviceRoleGroup);
                }
                LOGGER.debug("Starting CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().startClouderaManagerService(serviceComponent.getService(), true);
            } catch (Exception e) {
                LOGGER.warn("Unable to update and start CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
            }
        }
    }

    private int getAdditionalVolumeCount(Optional<Resource> optionalResource) {
        if (optionalResource.isPresent()) {
            Resource resource = optionalResource.get();
            LOGGER.info("Updating config for resources: {}", resource);
            VolumeSetAttributes volumeSetAttributes = resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class)
                    .orElseThrow(() -> new CloudbreakServiceException("Unable to get additional volumes information on resource"));
            return volumeSetAttributes.getVolumes().size();
        }
        return ZERO;
    }

    public void stopClouderaManagerServices(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents)
            throws CloudbreakServiceException {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Stopping CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().stopClouderaManagerService(serviceComponent.getService(), true);
            } catch (Exception e) {
                LOGGER.warn("Unable to stop CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakServiceException(String.format("Unable to stop CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    private Map<String, String> getConfigsForService(ServiceComponent serviceComponent, StackDto stackDto, String requestGroup) {
        String service = serviceComponent.getService().toLowerCase(Locale.ROOT);
        LOGGER.debug("Building configs to be updated for service {} in CM", service);
        Map<String, String> config = new HashMap<>();

        Optional<Resource> optionalResource = stackDto.getResources().stream().filter(resource -> null != resource.getInstanceGroup()
                && resource.getInstanceGroup().equals(requestGroup) && resource.getResourceType().toString()
                .contains("VOLUMESET")).findFirst();

        int additionalVolumeCount = getAdditionalVolumeCount(optionalResource);

        // Use the volume config providers for all services (YARN, IMPALA, HDFS, etc.)
        Optional<InstanceGroupDto> requestInstanceGroupOptional = stackDto.getInstanceGroupDtos().stream()
                .filter(group -> group.getInstanceGroup().getGroupName().equals(requestGroup)).findFirst();

        if (requestInstanceGroupOptional.isPresent()) {
            InstanceGroupDto instanceGroupDto = requestInstanceGroupOptional.get();
            HostgroupView hostgroupView = createHostgroupView(instanceGroupDto, additionalVolumeCount);
            TemplatePreparationObject templatePreparationObject = stackToTemplatePreparationObjectConverter.convert(stackDto);

            // Find the appropriate config provider for this service
            Optional<CmHostGroupRoleConfigProvider> configProvider = cmHostGroupRoleConfigProviders.stream()
                    .filter(provider -> service.equalsIgnoreCase(provider.getServiceType().toLowerCase(Locale.ROOT)))
                    .findFirst();

            if (configProvider.isPresent()) {
                CmHostGroupRoleConfigProvider provider = configProvider.get();
                config = provider.getConfigAfterAddingVolumes(hostgroupView, templatePreparationObject, serviceComponent);
            }
        }

        LOGGER.debug("Configs {} to be updated for service {} in CM", config, service);
        return config;
    }

    private HostgroupView createHostgroupView(InstanceGroupDto instanceGroupDto, int additionalVolumeCount) {
        Template template = instanceGroupDto.getInstanceGroup().getTemplate();
        String groupName = instanceGroupDto.getInstanceGroup().getGroupName();
        InstanceGroupType instanceGroupType = instanceGroupDto.getInstanceGroup().getInstanceGroupType();

        Set<String> hosts = instanceGroupDto.getNotDeletedAndNotZombieInstanceMetaData().stream()
                .map(InstanceMetadataView::getDiscoveryFQDN)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());

        Set<VolumeTemplate> volumeTemplates = template.getVolumeTemplates();

        return new HostgroupView(
                groupName,
                additionalVolumeCount,
                instanceGroupType,
                hosts,
                volumeTemplates,
                template.getTemporaryStorage(),
                template.getInstanceStorageCount(),
                template.getInstanceStorageSize()
        );
    }
}
