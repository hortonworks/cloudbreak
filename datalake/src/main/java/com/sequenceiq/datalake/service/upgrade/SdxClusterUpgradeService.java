package com.sequenceiq.datalake.service.upgrade;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.ImageInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionsV4Response;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class SdxClusterUpgradeService {

    private static final long WORKSPACE_ID = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxService sdxService;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    public UpgradeOptionsV4Response checkForClusterUpgradeByName(String name) {
        return stackV4Endpoint.checkForClusterUpgradeByName(0L, name);
    }

    public UpgradeOptionsV4Response checkForClusterUpgradeByCrn(String userCrn, String crn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        return stackV4Endpoint.checkForClusterUpgradeByName(WORKSPACE_ID, sdxCluster.getClusterName());
    }

    public FlowIdentifier triggerClusterUpgradeByName(String userCrn, String clusterName, String imageId) {
        SdxCluster cluster = sdxService.getSdxByNameInAccount(userCrn, clusterName);
        validateImageId(clusterName, imageId);
        MDCBuilder.buildMdcContext(cluster);
        return sdxReactorFlowManager.triggerDatalakeClusterUpgradeFlow(cluster.getId(), imageId);
    }

    public FlowIdentifier triggerClusterUpgradeByCrn(String userCrn, String clusterCrn, String imageId) {
        SdxCluster cluster = sdxService.getByCrn(userCrn, clusterCrn);
        validateImageId(cluster.getClusterName(), imageId);
        MDCBuilder.buildMdcContext(cluster);
        return sdxReactorFlowManager.triggerDatalakeClusterUpgradeFlow(cluster.getId(), imageId);
    }

    private void validateImageId(String clusterName, String imageId) {
        UpgradeOptionsV4Response upgradeOptionsV4Response = checkForClusterUpgradeByName(clusterName);
        List<ImageInfoV4Response> upgradeCandidates = upgradeOptionsV4Response.getUpgradeCandidates();
        if (CollectionUtils.isEmpty(upgradeCandidates)) {
            throw new BadRequestException(String.format("There is no compatible image to upgrade for stack %s", clusterName));
        } else if (upgradeCandidates.stream().noneMatch(imageInfoV4Response -> imageInfoV4Response.getImageId().equalsIgnoreCase(imageId))) {
            String candidates = upgradeCandidates.stream().map(ImageInfoV4Response::getImageId).collect(Collectors.joining(","));
            throw new BadRequestException(String.format("The given image (%s) is not eligible for upgrading the cluster. "
                    + "Please choose an id from the following image(s): %s", imageId, candidates));
        } else if (StringUtils.isNotEmpty(upgradeOptionsV4Response.getReason())) {
            throw new BadRequestException(String.format("The following error prevents the cluster upgrade process, please fix it and try again %s.",
            upgradeOptionsV4Response.getReason()));
        }
    }

}
