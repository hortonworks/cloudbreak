package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.TemporaryStorage.EPHEMERAL_VOLUMES_ONLY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Service
public class ConfigUpdateUtilService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUpdateUtilService.class);

    private static final String YARN_LOCAL_DIR = "yarn_nodemanager_local_dirs";

    private static final String YARN_LOG_DIR = "yarn_nodemanager_log_dirs";

    private static final String IMPALA_SCRATCH_DIR = "scratch_dirs";

    private static final String IMPALA_DATACACHE_DIR = "datacache_dirs";

    private static final String MOUNT_PATH_PREFIX_EPHFS = "/hadoopfs/ephfs";

    private static final String MOUNT_PATH_PREFIX_FS = "/hadoopfs/fs";

    private static final String YARN = "YARN";

    private static final String IMPALA = "IMPALA";

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    public void updateCMConfigsForComputeAndStartServices(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents,
            List<String> roleGroupNames, String requestGroup) throws CloudbreakServiceException {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Updating CM service config for service {}, in stack {} for roles {}", serviceComponent.getService(),
                        stackDto.getId(), roleGroupNames);
                Map<String, String> configMap = new HashMap<>();
                Set<String> allMountPoints = new HashSet<>();
                Optional<InstanceGroupDto> requestInstanceGroupOptional = stackDto.getInstanceGroupDtos().stream()
                        .filter(group -> group.getInstanceGroup().getGroupName().equals(requestGroup)).findFirst();
                if (requestInstanceGroupOptional.isPresent()) {
                    List<String> mountPointsForEphemeralVolumes = getEphemeralMountPoints(requestInstanceGroupOptional.get());
                    allMountPoints.addAll(mountPointsForEphemeralVolumes);
                }
                Optional<Resource> optionalResource = stackDto.getResources().stream().filter(resource -> null != resource.getInstanceGroup()
                        && resource.getInstanceGroup().equals(requestGroup) && resource.getResourceType().toString()
                        .contains("VOLUMESET")).findFirst();
                if (optionalResource.isPresent()) {
                    Resource resource = optionalResource.get();
                    LOGGER.info("Updating config for resources: {}", resource);
                    VolumeSetAttributes volumeSetAttributes = resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class)
                            .orElseThrow(() -> new CloudbreakServiceException("Unable to find fstab information on resource"));
                    String fstab = volumeSetAttributes.getFstab();
                    List<String> mountPointsFromFstab = Arrays.stream(StringUtils.substringsBetween(fstab, "/hadoopfs/", " "))
                            .map(str -> "/hadoopfs/" + str).toList();
                    allMountPoints.addAll(mountPointsFromFstab);
                }
                if (!CollectionUtils.isEmpty(allMountPoints)) {
                    configMap = getConfigsForService(serviceComponent.getService(), allMountPoints);
                    clusterApi.clusterModificationService().updateServiceConfig(serviceComponent.getService(), configMap, roleGroupNames);
                }
                LOGGER.debug("Starting CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().startClouderaManagerService(serviceComponent.getService(), false);
            } catch (Exception e) {
                LOGGER.error("Unable to update and start CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakServiceException(String.format("Unable to update and start CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    public void stopClouderaManagerServices(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents)
            throws CloudbreakServiceException {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Stopping CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().stopClouderaManagerService(serviceComponent.getService(), true);
            } catch (Exception e) {
                LOGGER.error("Unable to stop CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakServiceException(String.format("Unable to stop CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    private Map<String, String> getConfigsForService(String service, Set<String> mountPoints) {
        LOGGER.debug("Building configs to be updated for service {} in CM", service);
        Map<String, String> config = new HashMap<>();
        StringBuilder localMountPaths = new StringBuilder();
        StringBuilder logMountPaths = new StringBuilder();
        int counter = mountPoints.size();
        for (String mp : mountPoints) {
            setLocalAndLogMountPaths(localMountPaths, logMountPaths, service, mp, counter);
            counter--;
        }
        if (YARN.equalsIgnoreCase(service) && StringUtils.isNotEmpty(localMountPaths.toString())) {
            config.put(YARN_LOCAL_DIR, localMountPaths.toString());
            config.put(YARN_LOG_DIR, logMountPaths.toString());
        } else if (IMPALA.equalsIgnoreCase(service) && StringUtils.isNotEmpty(localMountPaths.toString())) {
            config.put(IMPALA_SCRATCH_DIR, localMountPaths.toString());
            config.put(IMPALA_DATACACHE_DIR, logMountPaths.toString());
        }
        LOGGER.debug("Configs {} to be updated for service {} in CM", config, service);
        return config;
    }

    private void setLocalAndLogMountPaths(StringBuilder localMountPaths, StringBuilder logMountPaths, String service, String mountPoint, int counter) {
        if (YARN.equalsIgnoreCase(service)) {
            localMountPaths.append(mountPoint).append("/nodemanager");
            logMountPaths.append(mountPoint).append("/nodemanager/log");
        } else if (IMPALA.equalsIgnoreCase(service)) {
            localMountPaths.append(mountPoint).append("/impala/scratch");
            logMountPaths.append(mountPoint).append("/impala/datacache");
        }
        if (counter - 1 > 0) {
            localMountPaths.append(',');
            logMountPaths.append(',');
        }
    }

    private List<String> getEphemeralMountPoints(InstanceGroupDto instanceGroup) {
        List<String> mountPointsForEphemeralVolumes = new ArrayList<>();
        Template template = instanceGroup.getInstanceGroup().getTemplate();
        if (null != template.getInstanceStorageCount() && template.getInstanceStorageCount() > 0) {
            for (int i = 1; i <= template.getInstanceStorageCount(); i++) {
                String mountPoint = null != template.getTemporaryStorage() && EPHEMERAL_VOLUMES_ONLY.equals(template.getTemporaryStorage()) ?
                        MOUNT_PATH_PREFIX_FS : MOUNT_PATH_PREFIX_EPHFS;
                mountPointsForEphemeralVolumes.add(mountPoint + i);
            }
        }
        return mountPointsForEphemeralVolumes;
    }
}
