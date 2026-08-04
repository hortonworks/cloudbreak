package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.DistroXOperationValidationResponse;
import com.sequenceiq.cloudbreak.sdx.common.model.DistroXOperationValidationView;

@Component
public class DistroXOperationValidationViewToResponseConverter {

    public DistroXOperationValidationResponse convert(DistroXOperationValidationView distroXOperationValidationView) {
        DistroXOperationValidationResponse distroXOperationValidationResponse = new DistroXOperationValidationResponse();
        distroXOperationValidationResponse.setOperation(distroXOperationValidationView.getOperation().toString());
        distroXOperationValidationResponse.setReason(distroXOperationValidationView.getReason());
        distroXOperationValidationResponse.setAllowed(distroXOperationValidationView.isAllowed());
        return distroXOperationValidationResponse;
    }
}
