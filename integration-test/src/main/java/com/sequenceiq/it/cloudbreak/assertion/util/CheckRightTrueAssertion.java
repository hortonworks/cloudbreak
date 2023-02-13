package com.sequenceiq.it.cloudbreak.assertion.util;

import java.util.Collection;
import java.util.Optional;

import com.sequenceiq.authorization.info.model.CheckRightV4SingleResponse;
import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckRightTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CheckRightTrueAssertion implements Assertion<CheckRightTestDto, CloudbreakClient> {

    @Override
    public CheckRightTestDto doAssertion(TestContext testContext, CheckRightTestDto testDto, CloudbreakClient client) throws Exception {
        testDto.getRightsToCheck().stream().forEach(rightV4 -> {
            checkRightSingleResponseAssertion(testDto.getResponse().getResponses(), rightV4);
        });
        return testDto;
    }

    protected static void checkRightSingleResponseAssertion(Collection<CheckRightV4SingleResponse> responseList, RightV4 rightV4) {
        Optional<CheckRightV4SingleResponse> checkRightV4SingleResponse = responseList.stream()
                .filter(response -> response.getRight().getAction().equals(rightV4.getAction())).findFirst();
        if (!checkRightV4SingleResponse.isPresent() || !checkRightV4SingleResponse.get().getResult()) {
            throw new TestFailException(String.format("Checking right for %s should have returned with true",
                    checkRightV4SingleResponse.get().getRight().getAction().getRight()));
        }
    }
}
