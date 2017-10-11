package com.sequenceiq.cloudbreak.controller;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v2.ClusterV2Endpoint;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJsonV2;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Controller
public class ClusterV2Controller implements ClusterV2Endpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterV2Controller.class);

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private ClusterCommonService clusterCommonService;

    @Autowired
    private StackService stackService;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public Response put(String name, UpdateClusterJsonV2 updateJson) throws Exception {
        IdentityUser user = authenticatedUserService.getCbUser();
        Stack publicStack = stackService.getPublicStack(name, user);
        updateJson.setStackId(publicStack.getId());
        updateJson.setAccount(user.getAccount());
        UpdateClusterJson convert = conversionService.convert(updateJson, UpdateClusterJson.class);
        return clusterCommonService.put(publicStack.getId(), convert);
    }
}
