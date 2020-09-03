package com.sequenceiq.it.cloudbreak.assertion.util;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckResourceRightV4SingleResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;

public class CheckResourceRightTrueAssertion implements Assertion<CheckResourceRightTestDto, CloudbreakClient> {

    @Override
    public CheckResourceRightTestDto doAssertion(TestContext testContext, CheckResourceRightTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.getRightsToCheck().entrySet().stream().forEach(entry -> {
            Optional<CheckResourceRightV4SingleResponse> resourceRightV4SingleResponse = testDto.getResponse().getResponses().stream()
                    .filter(checkResourceRightV4SingleResponse -> checkResourceRightV4SingleResponse.getResourceCrn().equals(entry.getKey())).findFirst();
            if (!resourceRightV4SingleResponse.isPresent()) {
                throw new AssertionError(String.format("Checking resource right for resource %s should have returned with a result", entry.getKey()));
            }
            entry.getValue().forEach(rightV4 -> {
                CheckRightTrueAssertion.checkRightSingleResponseAssertion(resourceRightV4SingleResponse.get().getRights(), rightV4);
            });
        });
        return testDto;
    }
}
