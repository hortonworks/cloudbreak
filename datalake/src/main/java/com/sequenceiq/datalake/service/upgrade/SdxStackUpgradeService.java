package com.sequenceiq.datalake.service.upgrade;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionsV4Response;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class SdxStackUpgradeService {

    private static final long WORKSPACE_ID = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxService sdxService;

    public UpgradeOptionsV4Response checkForStackUpgradeByName(String name) {
        return stackV4Endpoint.checkForStackUpgradeByName(0L, name);
    }

    public UpgradeOptionsV4Response checkForStackUpgradeByCrn(String userCrn, String crn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return stackV4Endpoint.checkForStackUpgradeByName(WORKSPACE_ID, sdxCluster.getClusterName());
    }

    public void upgradeStackByName(String name, String imageId) {
        FlowIdentifier flowIdentifier = stackV4Endpoint.upgradeStackByName(WORKSPACE_ID, name, imageId);
    }

    public void upgradeStackByCrn(String crn, String imageId, String userCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        stackV4Endpoint.upgradeStackByName(WORKSPACE_ID, sdxCluster.getClusterName(), imageId);
    }

}
