package com.sequenceiq.datalake.controller.sdx;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.cost.RealTimeCostResponse;
import com.sequenceiq.datalake.service.sdx.SdxCostService;
import com.sequenceiq.sdx.api.endpoint.SdxCostEndpoint;

@Controller
public class SdxCostController implements SdxCostEndpoint {

    @Inject
    private SdxCostService sdxCostService;

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public RealTimeCostResponse list(@ResourceCrnList List<String> sdxCrns) {
        return new RealTimeCostResponse(sdxCostService.getCosts(sdxCrns));
    }
}
