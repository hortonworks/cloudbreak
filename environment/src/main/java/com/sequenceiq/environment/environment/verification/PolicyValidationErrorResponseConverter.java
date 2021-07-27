package com.sequenceiq.environment.environment.verification;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CDPServicePolicyVerificationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.PolicyValidationErrorResponses;

@Component
public class PolicyValidationErrorResponseConverter {

    public PolicyValidationErrorResponses convert(CDPServicePolicyVerification cdpServicePolicyVerification) {
        PolicyValidationErrorResponses policyValidationErrorResponses = new PolicyValidationErrorResponses();
        Set<PolicyValidationErrorResponse> responses = new HashSet<>();

        for (CDPServicePolicyVerificationResponse entry : cdpServicePolicyVerification.getResults()) {
            PolicyValidationErrorResponse policyValidationErrorResponse = new PolicyValidationErrorResponse();
            policyValidationErrorResponse.setService(entry.getServiceName());
            policyValidationErrorResponse.setMessage(entry.getServiceStatus());
            policyValidationErrorResponse.setCode(entry.getStatusCode());
            responses.add(policyValidationErrorResponse);
        }
        policyValidationErrorResponses.setResponses(responses);
        return policyValidationErrorResponses;
    }
}
