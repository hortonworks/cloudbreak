package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;

@Service
public class ClusterUpgradePrerequisitesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePrerequisitesService.class);

    @Inject
    private ServicesToRemoveBeforeUpgrade servicesToRemoveBeforeUpgrade;

    public void removeIncompatibleServices(StackDto stackDto, ClusterApi connector, String targetRuntimeVersion) {
        servicesToRemoveBeforeUpgrade.getServicesToRemove().forEach((key, value) -> {
            try {
                removeServiceIfNecessary(stackDto, connector, targetRuntimeVersion, value, key);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to remove %s service from the cluster", key);
                LOGGER.error(errorMessage, e);
                throw new CloudbreakServiceException(errorMessage, e);
            }
        });
    }

    private void removeServiceIfNecessary(StackDto stackDto, ClusterApi connector, String targetRuntimeVersion, String versionLimit, String serviceType)
            throws Exception {
        if (StringUtils.hasText(targetRuntimeVersion) && isVersionNewerOrEqualThanLimited(targetRuntimeVersion, () -> versionLimit)) {
            LOGGER.debug("Trying to remove {} service from the cluster because this is a prerequisite of the target runtime version {}",
                    serviceType, targetRuntimeVersion);
            if (connector.isServicePresent(stackDto.getName(), serviceType)) {
                connector.stopClouderaManagerService(serviceType, true);
                connector.deleteClouderaManagerService(serviceType);
                LOGGER.debug("{} service successfully removed from the cluster.", serviceType);
            } else {
                LOGGER.debug("The {} service is not installed to the cluster.", serviceType);
            }
        } else {
            LOGGER.debug("Skipping the removal of {} service from the cluster because the target version ({}) is lower than {}}.",
                    serviceType, targetRuntimeVersion, versionLimit);
        }

    }
}
