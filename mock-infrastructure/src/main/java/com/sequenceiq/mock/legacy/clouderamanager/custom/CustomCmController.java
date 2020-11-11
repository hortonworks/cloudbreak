package com.sequenceiq.mock.legacy.clouderamanager.custom;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.swagger.model.ApiRemoteDataContext;

@Controller
@RequestMapping(value = "/api")
public class CustomCmController {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @RequestMapping(value = "/cdp/remoteContext/byCluster/{clusterName}",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public ResponseEntity<ApiRemoteDataContext> getRemoteContextByCluster(@PathVariable("clusterName") String clusterName) {
        return ProfileAwareResponse.exec(new ApiRemoteDataContext(), defaultModelService);
    }

    @RequestMapping(value = "/cdp/remoteContext",
            produces = { "application/json" },
            consumes = { "application/json" },
            method = RequestMethod.POST)
    public ResponseEntity<ApiRemoteDataContext> postRemoteContext(@Valid @RequestBody ApiRemoteDataContext body) {
        return ProfileAwareResponse.exec(new ApiRemoteDataContext(), defaultModelService);
    }
}
