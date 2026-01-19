package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaCheckVariantAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCheckVariantAction.class);

    private final String variant;

    public FreeIpaCheckVariantAction(String variant) {
        this.variant = variant;
    }

    @Override
    protected FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) {
        Log.when(LOGGER, " Checking FreeIpa variant, expected: " + variant);
        DescribeFreeIpaResponse response = client.getDefaultClient(testContext)
                .getFreeIpaV1Endpoint()
                .describe(testContext.given(EnvironmentTestDto.class).getCrn());
        if (!response.getVariant().equals(variant)) {
            throw new TestFailException("Variants are mismatched: expected: " + variant + ", got: " + response.getVariant());
        }
        Log.when(LOGGER, " FreeIpa variant checked and matched");
        return testDto;
    }
}
