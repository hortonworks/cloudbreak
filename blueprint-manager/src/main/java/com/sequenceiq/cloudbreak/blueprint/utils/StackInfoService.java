package com.sequenceiq.cloudbreak.blueprint.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.templateprocessor.processor.TemplateProcessingException;
import com.sequenceiq.cloudbreak.templateprocessor.templates.StackInfo;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;

@Component
public class StackInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInfoService.class);

    @Inject
    private BlueprintUtils blueprintUtils;

    public boolean hdfCluster(String blueprintText) {
        boolean hdfCluster;
        try {
            hdfCluster = "HDF".equals(blueprintStackInfo(blueprintText).getType().toUpperCase());
        } catch (TemplateProcessingException e) {
            hdfCluster = false;
        }
        return hdfCluster;
    }

    public StackInfo blueprintStackInfo(String blueprintText) throws TemplateProcessingException {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            return new StackInfo(blueprintUtils.getBlueprintHdpVersion(root), blueprintUtils.getBlueprintStackName(root));
        } catch (IOException e) {
            String message = String.format("Unable to detect BlueprintStackInfo from the source blueprint which was: %s.", blueprintText);
            LOGGER.warn(message);
            throw new TemplateProcessingException(message, e);
        }
    }

}
