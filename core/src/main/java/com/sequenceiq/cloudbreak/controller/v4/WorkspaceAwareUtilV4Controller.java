package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;
import org.testng.collections.Sets;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.BlueprintNameV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Responses;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;

@Controller
public class WorkspaceAwareUtilV4Controller extends NotificationController implements WorkspaceAwareUtilV4Endpoint {

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public ExposedServiceV4Responses getKnoxServices(Long workspaceId, BlueprintNameV4Filter blueprintNameV4Filter) {
        return new ExposedServiceV4Responses(Sets.newHashSet(serviceEndpointCollector.getKnoxServices(workspaceId, blueprintNameV4Filter.getBlueprintName())));
    }

}
