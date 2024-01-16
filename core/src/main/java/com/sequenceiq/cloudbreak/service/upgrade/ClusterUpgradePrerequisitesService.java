package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Service
public class ClusterUpgradePrerequisitesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradePrerequisitesService.class);

    private static final String DAS_SERVICE_TYPE = "DAS";

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public void removeDasServiceIfNecessary(StackDto stackDto, String targetRuntimeVersion) throws Exception {
        if (StringUtils.hasText(targetRuntimeVersion) && isVersionNewerOrEqualThanLimited(targetRuntimeVersion, CLOUDERA_STACK_VERSION_7_2_18)) {
            LOGGER.debug("Trying to remove DAS service from the cluster because this is a prerequisite of the target runtime version {}", targetRuntimeVersion);
            ClusterApi connector = clusterApiConnectors.getConnector(stackDto);
            if (connector.isServicePresent(stackDto.getName(), DAS_SERVICE_TYPE)) {
                connector.stopClouderaManagerService(DAS_SERVICE_TYPE);
                connector.deleteClouderaManagerService(DAS_SERVICE_TYPE);
                LOGGER.debug("DAS service successfully removed from the cluster.");
            } else {
                LOGGER.debug("The DAS service is not installed to the cluster.");
            }
        } else {
            LOGGER.debug("Skipping the removal of DAS service from the cluster because the target version ({}) is lower than 7.2.18.", targetRuntimeVersion);
        }

    }
}
