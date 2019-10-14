package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto.StackAccessDtoBuilder.aStackAccessDtoBuilder;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.v1.distrox.StackOperation;

@Controller
public class DistroXInternalV1Controller implements DistroXInternalV1Endpoint {

    @Inject
    private StackOperation stackOperation;

    @Override
    public StackViewV4Response getByCrn(String crn) {
        return stackOperation.getForInternalCrn(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                StackType.WORKLOAD);
    }
}
