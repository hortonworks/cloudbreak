package com.sequenceiq.freeipa.flow.freeipa.upscale.failure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;

public class UpscaleStackResultToUpscaleFailureEventConverterTest {
    private PayloadConverter<UpscaleFailureEvent> upscaleStackResultToUpscaleFailureEventConverter
            = new UpscaleStackResultToUpscaleFailureEventConverter();

    @Test
    void testCanConvertReturnsTrue() {
        assertTrue(new UpscaleStackResultToUpscaleFailureEventConverter().canConvert(UpscaleStackResult.class));
    }

    @Test
    void testCanConvertReturnsFalse() {
        assertFalse(new UpscaleStackResultToUpscaleFailureEventConverter().canConvert(CloudPlatformResult.class));
    }

    @Test
    void testConvertReturnsFailureDetails() {
        UpscaleStackResult upscaleStackResult = populateUpscaleStackResult("Error occurred during upscale");
        UpscaleFailureEvent upscaleFailureEvent = upscaleStackResultToUpscaleFailureEventConverter.convert(upscaleStackResult);
        assertEquals(upscaleFailureEvent.getResourceId(), upscaleStackResult.getResourceId());
        assertEquals(upscaleFailureEvent.getFailedPhase(), "Adding instances");
        assertEquals(upscaleFailureEvent.getSuccess().size(), 0);
        assertEquals(upscaleFailureEvent.getFailureDetails().size(), 1);
        assertTrue(upscaleFailureEvent.getFailureDetails().containsKey("statusReason"));
        assertEquals(upscaleFailureEvent.getFailureDetails().get("statusReason"), upscaleStackResult.getStatusReason());
        assertEquals(upscaleFailureEvent.getException(), upscaleStackResult.getException());
    }

    @Test
    void testConvertDoesNotReturnFailureDetails() {
        UpscaleStackResult upscaleStackResult = populateUpscaleStackResult(null);
        UpscaleFailureEvent upscaleFailureEvent = upscaleStackResultToUpscaleFailureEventConverter.convert(upscaleStackResult);
        assertEquals(upscaleFailureEvent.getResourceId(), upscaleStackResult.getResourceId());
        assertEquals(upscaleFailureEvent.getFailedPhase(), "Adding instances");
        assertEquals(upscaleFailureEvent.getSuccess().size(), 0);
        assertEquals(upscaleFailureEvent.getFailureDetails().size(), 0);
        assertEquals(upscaleFailureEvent.getException(), upscaleStackResult.getException());
    }

    private UpscaleStackResult populateUpscaleStackResult(String statusReason) {
        return new UpscaleStackResult(statusReason,
                new Exception(statusReason), 1234L);
    }

}