package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class SdxImageCatalogService {

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public void changeImageCatalog(SdxCluster sdxCluster, String imageCatalog) {
        if (flowLogService.isOtherFlowRunning(sdxCluster.getId())) {
            throw new FlowsAlreadyRunningException(String.format("Operation is running for cluster '%s'. Please try again later.", sdxCluster.getName()));
        }
        ChangeImageCatalogV4Request changeImageCatalogRequest = new ChangeImageCatalogV4Request();
        changeImageCatalogRequest.setImageCatalog(imageCatalog);
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.changeImageCatalogInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getClusterName(), initiatorUserCrn, changeImageCatalogRequest));
    }

    public CloudbreakImageCatalogV3 generateImageCatalog(String name) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                stackV4Endpoint.generateImageCatalogInternal(WORKSPACE_ID_DEFAULT, name, initiatorUserCrn).getImageCatalog());
    }
}
