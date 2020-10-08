package com.sequenceiq.cloudbreak.service.upgrade.matrix;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Component
public class UpgradeMatrixDefinitionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeMatrixDefinitionProvider.class);

    private static final String UPGRADE_MATRIX_DEFINITION_FILE = "upgrade-matrix-definition";

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    public UpgradeMatrixDefinition getUpgradeMatrix() throws IOException {
        LOGGER.debug("Reading {}.json from file system.", UPGRADE_MATRIX_DEFINITION_FILE);
        String upgradeMatrixJson = cloudbreakResourceReaderService.resourceDefinition(UPGRADE_MATRIX_DEFINITION_FILE);
        return convertJsonToObject(upgradeMatrixJson);
    }

    private UpgradeMatrixDefinition convertJsonToObject(String upgradeMatrixJson) throws IOException {
        LOGGER.debug("Converting upgrade matrix json into object.");
        return JsonUtil.readValue(upgradeMatrixJson, UpgradeMatrixDefinition.class);
    }
}
