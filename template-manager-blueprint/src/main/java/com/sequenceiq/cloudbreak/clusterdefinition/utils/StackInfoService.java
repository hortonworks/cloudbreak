package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.model.ClusterDefinitionStackInfo;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class StackInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackInfoService.class);

    @Inject
    private AmbariBlueprintUtils ambariBlueprintUtils;

    public boolean isHdfCluster(String blueprintText) {
        boolean hdfCluster;
        try {
            hdfCluster = "HDF".equalsIgnoreCase(clusterDefinitionStackInfo(blueprintText).getType());
        } catch (ClusterDefinitionProcessingException e) {
            hdfCluster = false;
        }
        return hdfCluster;
    }

    public ClusterDefinitionStackInfo clusterDefinitionStackInfo(String blueprintText) {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            return new ClusterDefinitionStackInfo(ambariBlueprintUtils.getBlueprintStackVersion(root), ambariBlueprintUtils.getBlueprintStackName(root));
        } catch (IOException e) {
            String message = String.format("Unable to detect ClusterDefinitionStackInfo from the source cluster definition which was: %s.", blueprintText);
            LOGGER.warn(message);
            throw new ClusterDefinitionProcessingException(message, e);
        }
    }

}
