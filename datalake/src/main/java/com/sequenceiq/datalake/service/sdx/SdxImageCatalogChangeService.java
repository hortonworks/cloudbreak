package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class SdxImageCatalogChangeService {

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    public void changeImageCatalog(SdxCluster sdxCluster, String imageCatalog) {
        if (flowLogService.isOtherFlowRunning(sdxCluster.getId())) {
            throw new CloudbreakApiException(String.format("Operation is running for cluster '%s'. Please try again later.", sdxCluster.getName()));
        }
        try {
            ChangeImageCatalogV4Request changeImageCatalogRequest = new ChangeImageCatalogV4Request();
            changeImageCatalogRequest.setImageCatalog(imageCatalog);
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                    stackV4Endpoint.changeImageCatalogInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getClusterName(), initiatorUserCrn, changeImageCatalogRequest));
        } catch (CloudbreakServiceException e) {
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }
}
