package com.sequenceiq.mock.clouderamanager.custom;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;

@Controller
@RequestMapping(value = "/{mockUuid}/api")
public class CustomCmController {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @RequestMapping(value = "/cdp/remoteContext/byCluster/{clusterName}",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(@PathVariable("mockUuid") String mockUuid,
            @PathVariable("clusterName") String clusterName) {
        return responseCreatorComponent.exec(new ApiRemoteDataContext());
    }

    @RequestMapping(value = "/cdp/remoteContext",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(@PathVariable("mockUuid") String mockUuid, @Valid @RequestBody ApiRemoteDataContext body) {
        return responseCreatorComponent.exec(new ApiRemoteDataContext());
    }
}
