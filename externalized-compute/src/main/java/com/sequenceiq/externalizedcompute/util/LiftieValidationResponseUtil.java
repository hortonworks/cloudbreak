package com.sequenceiq.externalizedcompute.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftieshared.LiftieSharedProto.ValidationResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class LiftieValidationResponseUtil {

    public void throwException(ValidationResponse validationResponse) {
        if (validationResponse != null) {
            Set<String> failedValidationMessages = new LinkedHashSet<>();
            validationResponse.getValidationsList().forEach(validation -> {
                if ("FAILED".equals(validation.getStatus())) {
                    String error = validation.getMessage();
                    String errorReason = validation.getDetailedMessage();
                    if (StringUtils.isNotEmpty(error)) {
                        failedValidationMessages.add("Error: " + error + " Reason: " + errorReason);
                    }
                }
            });
            if (!failedValidationMessages.isEmpty()) {
                throw new CloudbreakServiceException("Validation failed: " + String.join(" ", failedValidationMessages));
            }
        }
    }
}
