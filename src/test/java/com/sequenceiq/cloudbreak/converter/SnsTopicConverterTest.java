package com.sequenceiq.cloudbreak.converter;

import com.amazonaws.regions.Regions;
import com.sequenceiq.cloudbreak.controller.json.SnsTopicJson;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import org.junit.Before;
import org.junit.Test;
import com.amazonaws.services.sqs.model.UnsupportedOperationException;

import static org.junit.Assert.assertEquals;

public class SnsTopicConverterTest {

    private static final String DUMMY_TOPIC_ARN = "dummyTopicArn";

    private SnsTopicConverter underTest;

    private SnsTopic snsTopic;

    @Before
    public void setUp() {
        underTest = new SnsTopicConverter();
        snsTopic = createSnsTopic();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConvertSnsTopicJsonToEntity() {
        // GIVEN
        // WHEN
        underTest.convert(new SnsTopicJson());
    }

    @Test
    public void testConvertSnsTopicEntityToJson() {
        // GIVEN
        // WHEN
        SnsTopicJson result = underTest.convert(snsTopic);
        // THEN
        assertEquals(result.getId(), snsTopic.getId());
        assertEquals(result.getName(), snsTopic.getName());
        assertEquals(result.getRegion(), snsTopic.getRegion());
        assertEquals(result.getTopicArn(), snsTopic.getTopicArn());
    }

    private SnsTopic createSnsTopic() {
        SnsTopic snsTopic = new SnsTopic();
        snsTopic.setConfirmed(true);
        snsTopic.setCredential(new AwsCredential());
        snsTopic.setId(1L);
        snsTopic.setRegion(Regions.DEFAULT_REGION);
        snsTopic.setTopicArn(DUMMY_TOPIC_ARN);
        return snsTopic;
    }
}
