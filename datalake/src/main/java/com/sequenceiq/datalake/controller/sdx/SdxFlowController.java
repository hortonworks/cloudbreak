package com.sequenceiq.datalake.controller.sdx;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.sdx.api.endpoint.SdxFlowEndpoint;

@Controller
@InternalOnly
public class SdxFlowController extends FlowController implements SdxFlowEndpoint {
}