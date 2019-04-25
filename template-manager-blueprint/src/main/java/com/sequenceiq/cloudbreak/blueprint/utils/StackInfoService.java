package com.sequenceiq.cloudbreak.blueprint.utils;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class StackInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInfoService.class);

    @Inject
    private BlueprintUtils blueprintUtils;

    public boolean isHdfCluster(String blueprintText) {
        boolean hdfCluster;
        try {
            hdfCluster = "HDF".equalsIgnoreCase(blueprintStackInfo(blueprintText).getType());
        } catch (BlueprintProcessingException e) {
            hdfCluster = false;
        }
        return hdfCluster;
    }

    public BlueprintStackInfo blueprintStackInfo(String blueprintText) {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            return new BlueprintStackInfo(blueprintUtils.getBlueprintStackVersion(root), blueprintUtils.getBlueprintStackName(root));
        } catch (IOException e) {
            String message = String.format("Unable to detect BlueprintStackInfo from the source blueprint which was: %s.", blueprintText);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
    }

}
