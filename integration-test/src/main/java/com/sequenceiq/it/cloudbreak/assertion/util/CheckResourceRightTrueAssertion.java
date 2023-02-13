package com.sequenceiq.it.cloudbreak.assertion.util;

import java.util.Optional;

import com.sequenceiq.authorization.info.model.CheckResourceRightV4SingleResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CheckResourceRightTrueAssertion implements Assertion<CheckResourceRightTestDto, CloudbreakClient> {

    @Override
    public CheckResourceRightTestDto doAssertion(TestContext testContext, CheckResourceRightTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.getRightsToCheck().entrySet().stream().forEach(entry -> {
            Optional<CheckResourceRightV4SingleResponse> resourceRightV4SingleResponse = testDto.getResponse().getResponses().stream()
                    .filter(checkResourceRightV4SingleResponse -> checkResourceRightV4SingleResponse.getResourceCrn().equals(entry.getKey())).findFirst();
            if (!resourceRightV4SingleResponse.isPresent()) {
                throw new TestFailException(String.format("Checking resource right for resource %s should have returned with a result", entry.getKey()));
            }
            entry.getValue().forEach(rightV4 -> {
                CheckRightTrueAssertion.checkRightSingleResponseAssertion(resourceRightV4SingleResponse.get().getRights(), rightV4);
            });
        });
        return testDto;
    }
}
