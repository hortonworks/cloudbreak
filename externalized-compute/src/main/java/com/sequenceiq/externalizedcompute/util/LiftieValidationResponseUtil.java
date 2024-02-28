package com.sequenceiq.externalizedcompute.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.liftiepublic.LiftiePublicProto;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class LiftieValidationResponseUtil {

    public void throwException(LiftiePublicProto.ValidationResponse validationResponse) {
        if (validationResponse != null) {
            Set<String> failedValidationMessages = new LinkedHashSet<>();
            validationResponse.getValidationsList().forEach(validation -> {
                if ("FAILED".equals(validation.getStatus())) {
                    String message = validation.getMessage();
                    if (StringUtils.isNoneEmpty(message)) {
                        failedValidationMessages.add(message);
                    }
                }
            });
            if (!failedValidationMessages.isEmpty()) {
                throw new CloudbreakServiceException("Validation failed: " + String.join(" ", failedValidationMessages));
            }
        }
    }
}
