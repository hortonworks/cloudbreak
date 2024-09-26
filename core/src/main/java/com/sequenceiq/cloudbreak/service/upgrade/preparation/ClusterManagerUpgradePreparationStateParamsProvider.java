package com.sequenceiq.cloudbreak.service.upgrade.preparation;

import static java.util.Collections.singletonMap;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.converter.ImageToClouderaManagerRepoConverter;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Component
public class ClusterManagerUpgradePreparationStateParamsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpgradePreparationStateParamsProvider.class);

    private static final String PILLAR_KEY = "cloudera-manager-upgrade-prepare";

    @Inject
    private ImageToClouderaManagerRepoConverter imageToClouderaManagerRepoConverter;

    public Map<String, SaltPillarProperties> createParamsForCmPackageDownload(Image candidateImage) {
        ClouderaManagerRepo clouderaManagerRepo = imageToClouderaManagerRepoConverter.convert(candidateImage);
        LOGGER.debug("Adding Cloudera Manager repo to Salt pillar: {}", clouderaManagerRepo);
        return singletonMap(PILLAR_KEY, new SaltPillarProperties("/cloudera-manager/repo-prepare.sls",
                singletonMap(PILLAR_KEY, Map.of(
                        "repo", clouderaManagerRepo))));
    }

}
