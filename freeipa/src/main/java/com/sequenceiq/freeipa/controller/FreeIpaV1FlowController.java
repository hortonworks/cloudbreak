package com.sequenceiq.freeipa.controller;


import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.flow.controller.FlowController;
import com.sequenceiq.freeipa.api.v1.freeipa.flow.FreeIpaV1FlowEndpoint;

@Controller
@InternalOnly
public class FreeIpaV1FlowController extends FlowController implements FreeIpaV1FlowEndpoint {
}
