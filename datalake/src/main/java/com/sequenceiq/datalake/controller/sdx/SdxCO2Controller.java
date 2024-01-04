package com.sequenceiq.datalake.controller.sdx;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.common.co2.RealTimeCO2Response;
import com.sequenceiq.datalake.service.sdx.SdxCostService;
import com.sequenceiq.sdx.api.endpoint.SdxCO2Endpoint;

@Controller
public class SdxCO2Controller implements SdxCO2Endpoint {

    @Inject
    private SdxCostService sdxCostService;

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.DESCRIBE_DATALAKE)
    public RealTimeCO2Response list(@ResourceCrnList List<String> sdxCrns) {
        return new RealTimeCO2Response(sdxCostService.getCO2(sdxCrns));
    }
}
