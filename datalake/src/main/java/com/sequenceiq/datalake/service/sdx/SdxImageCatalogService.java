package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.imagecatalog.GenerateImageCatalogV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class SdxImageCatalogService {

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void changeImageCatalog(SdxCluster sdxCluster, String imageCatalog) {
        if (flowLogService.isOtherFlowRunning(sdxCluster.getId())) {
            throw new CloudbreakApiException(String.format("Operation is running for cluster '%s'. Please try again later.", sdxCluster.getName()));
        }
        try {
            ChangeImageCatalogV4Request changeImageCatalogRequest = new ChangeImageCatalogV4Request();
            changeImageCatalogRequest.setImageCatalog(imageCatalog);
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () ->
                    stackV4Endpoint.changeImageCatalogInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getClusterName(), initiatorUserCrn, changeImageCatalogRequest));
        } catch (CloudbreakServiceException e) {
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }

    public CloudbreakImageCatalogV3 generateImageCatalog(String name) {
        try {
            String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> {
                GenerateImageCatalogV4Response response = stackV4Endpoint.generateImageCatalogInternal(WORKSPACE_ID_DEFAULT, name, initiatorUserCrn);
                return response.getImageCatalog();
            });
        } catch (CloudbreakServiceException e) {
            throw new CloudbreakApiException(e.getMessage(), e);
        }
    }
}
