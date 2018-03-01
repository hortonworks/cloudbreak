package com.sequenceiq.cloudbreak.blueprint.utils;

import java.io.IOException;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.blueprint.templates.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class StackInfoService {

    @Inject
    private BlueprintUtils blueprintUtils;

    public boolean hdfCluster(StackRepoDetails source) {
        String repoId = source.getStack().get("repoid");
        if (repoId.toLowerCase().startsWith("hdf")) {
            return true;
        }
        return false;
    }

    public BlueprintStackInfo blueprintStackInfo(String blueprintText) throws IOException {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            return new BlueprintStackInfo(blueprintUtils.getBlueprintHdpVersion(root), blueprintUtils.getBlueprintStackName(root));
        } catch (IOException e) {
            throw e;
        }
    }

}
