package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static java.util.Collections.singletonMap;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.OsChangeService;
import com.sequenceiq.common.model.OsType;

@Component
public class ClusterManagerUpgradePreparationStateParamsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradePreparationStateParamsProvider.class);

    private static final String PILLAR_KEY = "cloudera-manager-upgrade-prepare";

    @Inject
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    @Inject
    private OsChangeService osChangeService;

    @Inject
    private StackImageService stackImageService;

    public Map<String, SaltPillarProperties> createParamsForCmPackageDownload(Image candidateImage, Long stackId) {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = getCurrentImage(stackId);
        OsType currentOsType = OsType.getByOsTypeString(currentImage.getOsType());
        OsType targetOsType = OsType.getByOsTypeString(candidateImage.getOsType());
        String currentArchitecture = currentImage.getArchitecture();
        ClouderaManagerRepo clouderaManagerRepo = imageToClouderaManagerRepoConverter.convert(candidateImage);
        clouderaManagerRepo = osChangeService.updateCmRepoInCaseOfOsChange(clouderaManagerRepo, currentOsType, targetOsType, currentArchitecture);
        LOGGER.debug("Adding Cloudera Manager repo to Salt pillar: {}", clouderaManagerRepo);
        return singletonMap(PILLAR_KEY, new SaltPillarProperties("/cloudera-manager/repo-prepare.sls",
                singletonMap(PILLAR_KEY, Map.of(
                        "repo", clouderaManagerRepo))));
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image getCurrentImage(Long stackId) {
        try {
            return stackImageService.getCurrentImage(stackId);
        } catch (CloudbreakImageNotFoundException e) {
            throw new NotFoundException("Image not found for stack", e);
        }
    }

}
