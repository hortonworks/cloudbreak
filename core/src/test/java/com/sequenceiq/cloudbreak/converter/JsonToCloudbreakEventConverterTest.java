package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.model.CloudbreakEventsJson;

public class JsonToCloudbreakEventConverterTest extends AbstractJsonConverterTest<CloudbreakEventsJson> {

    private JsonToCloudbreakEventConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToCloudbreakEventConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CloudbreakEvent result = underTest.convert(getRequest("event/cloudbreak-event.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }


    @Override
    public Class<CloudbreakEventsJson> getRequestClass() {
        return CloudbreakEventsJson.class;
    }
}
