package com.sequenceiq.mock.clouderamanager.base;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;

@Controller
public class CdpResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(String mockUuid, String clusterName) {
        return responseCreatorComponent.exec(new ApiRemoteDataContext());
    }

    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(String mockUuid, @Valid ApiRemoteDataContext body) {
        return responseCreatorComponent.exec(new ApiRemoteDataContext());
    }
}
