package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;

import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CheckVariant implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = getLogger(CheckVariant.class);

    private final String variant;

    public CheckVariant(String variant) {
        this.variant = variant;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, " Checking the stack variant, expected: " + variant);
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(testDto.getName(), Collections.emptySet());
        if (!stackV4Response.getVariant().equals(variant)) {
            throw new TestFailException("Variants are mismatched: expected: " + variant + ", got: " + stackV4Response.getVariant());
        }
        Log.when(LOGGER, " Stack variant checked and matched");
        return testDto;
    }
}
