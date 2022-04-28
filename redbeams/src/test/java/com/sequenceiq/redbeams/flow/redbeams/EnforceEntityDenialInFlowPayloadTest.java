package com.sequenceiq.redbeams.flow.redbeams;

import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.EnforceEntityDenialUtil;

public class EnforceEntityDenialInFlowPayloadTest {

    @Test
    public void enforceDenialOfEntitiesInFlowPayload() {
        EnforceEntityDenialUtil.denyEntity();
    }
}
