package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;

public class CloudbreakEventToJsonEntityConverterTest extends AbstractEntityConverterTest<CloudbreakEvent> {
    private CloudbreakEventToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new CloudbreakEventToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CloudbreakEventsJson result = underTest.convert(getSource());
        // THEN
        assertEquals("message", result.getEventMessage());
        assertAllFieldsNotNull(result);
    }

    @Override
    public CloudbreakEvent createSource() {
        return TestUtil.azureCloudbreakEvent(1L);
    }
}
