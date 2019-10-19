package com.sequenceiq.cloudbreak.cmtemplate.utils;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Component
public class StackInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInfoService.class);

    @Inject
    private BlueprintUtils blueprintUtils;

    public BlueprintStackInfo blueprintStackInfo(String blueprintText) {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            return new BlueprintStackInfo(blueprintUtils.getBlueprintStackVersion(root), blueprintUtils.getBlueprintStackName(root));
        } catch (IOException e) {
            String message = String.format("Unable to detect ClusterTemplateStackInfo from the source cluster template which was: %s.", blueprintText);
            LOGGER.warn(message);
            throw new BlueprintProcessingException(message, e);
        }
    }

}
