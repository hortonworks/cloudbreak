package com.sequenceiq.mock.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;
import com.sequenceiq.mock.swagger.v40.api.CdpResourceApi;

@Controller
public class CdpResourceV40Controller implements CdpResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Override
    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(String mockUuid, String clusterName) {
        return profileAwareComponent.exec(new ApiRemoteDataContext());
    }

    @Override
    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(String mockUuid, @Valid ApiRemoteDataContext body) {
        return profileAwareComponent.exec(new ApiRemoteDataContext());
    }
}
