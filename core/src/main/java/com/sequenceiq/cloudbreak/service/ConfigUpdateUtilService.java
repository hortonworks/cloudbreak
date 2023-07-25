package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

@Service
public class ConfigUpdateUtilService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigUpdateUtilService.class);

    private static final int MAX_READ_COUNT = 15;

    private static final int SLEEP_INTERVAL = 10;

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

    public void updateCMConfigsForComputeAndStartServices(StackDto stackDto, Set<ServiceComponent> hostTemplateServiceComponents,
            int instanceStorageCount, int attachedVolumesCount, List<String> roleGroupNames, TemporaryStorage temporaryStorage)
            throws CloudbreakServiceException {
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
        for (ServiceComponent serviceComponent : hostTemplateServiceComponents) {
            try {
                LOGGER.debug("Updating CM service config for service {}, in stack {} for roles {}", serviceComponent.getService(),
                        stackDto.getId(), roleGroupNames);
                Map<String, String> configMap = getConfigsForService(instanceStorageCount, attachedVolumesCount,
                        serviceComponent.getService(), temporaryStorage);
                if (!CollectionUtils.isEmpty(configMap)) {
                    clusterApi.clusterModificationService().updateServiceConfig(serviceComponent.getService(), configMap, roleGroupNames);
                }
                LOGGER.debug("Starting CM service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                clusterApi.clusterModificationService().startClouderaManagerService(serviceComponent.getService());
                pollClouderaManagerServices(clusterApi, serviceComponent.getService(), "STARTED");
            } catch (Exception e) {
                LOGGER.error("Unable to start CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakServiceException(String.format("Unable to start CM services for " +
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
                clusterApi.clusterModificationService().stopClouderaManagerService(serviceComponent.getService());
                pollClouderaManagerServices(clusterApi, serviceComponent.getService(), "STOPPED");
            } catch (Exception e) {
                LOGGER.error("Unable to stop CM services for service {}, in stack {}", serviceComponent.getService(), stackDto.getId());
                throw new CloudbreakServiceException(String.format("Unable to stop CM services for " +
                        "service %s, in stack %s: %s", serviceComponent.getService(), stackDto.getId(), e.getMessage()));
            }
        }
    }

    private Map<String, String> getConfigsForService(int instanceStorageCount, int attachedVolumesCount, String service,
            TemporaryStorage temporaryStorage) {
        LOGGER.debug("Building configs to be updated for service {} in CM", service);
        Map<String, String> config = new HashMap<>();
        StringBuilder localMountPaths = new StringBuilder();
        StringBuilder logMountPaths = new StringBuilder();
        int mountPathIndex = 1;
        String mountPathPrefix = temporaryStorage.equals(TemporaryStorage.EPHEMERAL_VOLUMES) ? MOUNT_PATH_PREFIX_EPHFS : MOUNT_PATH_PREFIX_FS;
        int volumeCounter = instanceStorageCount == 0 ? attachedVolumesCount : instanceStorageCount;
        while (volumeCounter > 0) {
            setLocalAndLogMountPaths(localMountPaths, logMountPaths, service, mountPathPrefix, volumeCounter, mountPathIndex);
            mountPathIndex++;
            volumeCounter--;
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

    private void setLocalAndLogMountPaths(StringBuilder localMountPaths, StringBuilder logMountPaths, String service, String mountPathPrefix,
            int volumeCounter, int mountPathIndex) {
        if (YARN.equalsIgnoreCase(service)) {
            localMountPaths.append(mountPathPrefix).append(mountPathIndex).append("/nodemanager");
            logMountPaths.append(mountPathPrefix).append(mountPathIndex).append("/nodemanager/log");
        } else if (IMPALA.equalsIgnoreCase(service)) {
            localMountPaths.append(mountPathPrefix).append(mountPathIndex).append("/impala/scratch");
            logMountPaths.append(mountPathPrefix).append(mountPathIndex).append("/impala/datacache");
        }
        if (volumeCounter - 1 > 0) {
            localMountPaths.append(',');
            logMountPaths.append(',');
        }
    }

    private void pollClouderaManagerServices(ClusterApi clusterApi, String service, String status) throws Exception {
        LOGGER.debug("Starting polling on CM Service {} to check if {}", service, status);
        Polling.waitPeriodly(SLEEP_INTERVAL, TimeUnit.SECONDS).stopIfException(true).stopAfterAttempt(MAX_READ_COUNT)
            .run(() -> {
                LOGGER.debug("Polling CM Service {} to check if {}", service, status);
                Map<String, String> readResults = clusterApi.clusterModificationService().fetchServiceStatuses();
                if (status.equals(readResults.get(service.toLowerCase(Locale.ROOT)))) {
                    return AttemptResults.justFinish();
                }
                return AttemptResults.justContinue();
            });
    }
}