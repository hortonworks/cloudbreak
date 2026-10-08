package com.sequenceiq.cloudbreak.core.cluster.prerequisite;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.StackDto;

@Service
public class ClouderaManagerUpgradePrerequisiteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUpgradePrerequisiteService.class);

    @Inject
    private List<ClouderaManagerUpgradePrerequisite> prerequisites;

    public void executePrerequisites(StackDto stackDto) {
        if (CollectionUtils.isEmpty(prerequisites)) {
            LOGGER.debug("There is no Cloudera Manager upgrade prerequisite to execute.");
            return;
        }
        LOGGER.debug("Executing the following Cloudera Manager upgrade prerequisites: {}", prerequisites);
        prerequisites.forEach(prerequisite -> prerequisite.execute(stackDto));
    }
}
