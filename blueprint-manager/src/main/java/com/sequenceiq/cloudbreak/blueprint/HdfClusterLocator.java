package com.sequenceiq.cloudbreak.blueprint;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;

@Component
public class HdfClusterLocator {

    public boolean hdfCluster(StackRepoDetails source) {
        String repoId = source.getStack().get("repoid");
        if (repoId.toLowerCase().startsWith("hdf")) {
            return true;
        }
        return false;
    }
}
