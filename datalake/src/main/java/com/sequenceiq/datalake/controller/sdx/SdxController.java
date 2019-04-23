package com.sequenceiq.datalake.controller.sdx;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.datalake.api.endpoint.sdx.RedeploySdxClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterRequest;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxClusterResponse;
import com.sequenceiq.datalake.api.endpoint.sdx.SdxEndpoint;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.util.RestRequestThreadLocalService;

@Controller
public class SdxController implements SdxEndpoint {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private SdxService sdxService;

    @Override
    public SdxClusterResponse create(String envName, @Valid SdxClusterRequest createSdxClusterRequest) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        return sdxService.createSdx(userCrn, envName, createSdxClusterRequest);
    }

    @Override
    public void delete(String envName) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        sdxService.deleteSdx(userCrn, envName);
    }

    @Override
    public void redeploy(String envName, @Valid RedeploySdxClusterRequest redeploySdxClusterRequest) {

    }

    @Override
    public SdxClusterResponse get(String envName) {
        return null;
    }

    @Override
    public List<SdxClusterResponse> list(String envName) {
        String userCrn = restRequestThreadLocalService.getUserCrn();
        return sdxService.listSdx(userCrn, envName);
    }

}
