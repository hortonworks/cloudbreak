package com.sequenceiq.mock.clouderamanager.v40.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.CdpResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;
import com.sequenceiq.mock.swagger.v40.api.CdpResourceApi;

@Controller
public class CdpResourceV40Controller implements CdpResourceApi {

    @Inject
    private CdpResourceOperation cdpResourceOperation;

    @Override
    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(String mockUuid, String clusterName) {
        return cdpResourceOperation.getRemoteContextByCluster(mockUuid, clusterName);
    }

    @Override
    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(String mockUuid, ApiRemoteDataContext body) {
        return cdpResourceOperation.postRemoteContext(mockUuid, body);
    }
}
