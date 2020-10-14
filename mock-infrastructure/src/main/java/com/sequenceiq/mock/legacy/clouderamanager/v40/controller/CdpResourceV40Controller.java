package com.sequenceiq.mock.legacy.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.swagger.v40.api.CdpResourceApi;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;

@Controller
public class CdpResourceV40Controller implements CdpResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiRemoteDataContext> getRemoteContext(String dataContextName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(String clusterName) {
        return ProfileAwareResponse.exec(new ApiRemoteDataContext(), defaultModelService);
    }

    @Override
    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(@Valid ApiRemoteDataContext body) {
        return ProfileAwareResponse.exec(new ApiRemoteDataContext(), defaultModelService);
    }
}
